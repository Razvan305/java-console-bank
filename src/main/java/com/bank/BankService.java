package com.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class BankService {
    public final DataBaseConfig dbConfig;

    public void createAccount(String accNum, String holderName, BigDecimal initialDeposit) throws SQLException {
        if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }

        String insertAcc = "INSERT INTO accounts (account_number, holder_name, balance) VALUES (?, ?, ?)";
        try (Connection conn = dbConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stm = conn.prepareStatement(insertAcc)) {
                stm.setString(1, accNum);
                stm.setString(2, holderName);
                stm.setBigDecimal(3, initialDeposit);
                stm.executeUpdate();

                if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    logTransaction(conn, accNum, "DEPOSIT", initialDeposit, null);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;

            }
        }
    }

    public void deposit(String accNum, BigDecimal amount) throws SQLException {
        validatePositiveAmount(amount);

        String deposit = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (Connection conn = dbConfig.getConnection()){
            conn.setAutoCommit(false);
            try (PreparedStatement stm = conn.prepareStatement(deposit)) {
                stm.setBigDecimal(1, amount);
                stm.setString(2, accNum);
                if (stm.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Account not found");
                }
                logTransaction(conn, accNum, "DEPOSIT", amount, null);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void withdraw(String accNum, BigDecimal amount) throws SQLException {
        validatePositiveAmount(amount);

        try (Connection conn = dbConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal currentBalance = getBalanceInternal(conn, accNum);
                if (currentBalance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient founds.");
                }

                String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                try (PreparedStatement stm = conn.prepareStatement(updateSql)){
                    stm.setBigDecimal(1, amount);
                    stm.setString(2, accNum);
                    stm.executeUpdate();
                }

                logTransaction(conn, accNum, "WITHDRAW", amount, null);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void trasnfer(String fromAcc, String toAcc, BigDecimal amount) throws SQLException {
        validatePositiveAmount(amount);

        try (Connection conn = dbConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                BigDecimal senderBalance = getBalanceInternal(conn, fromAcc);
                if (senderBalance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient founds.");
                }

                String deductSQL = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                try (PreparedStatement stm = conn.prepareStatement(deductSQL)) {
                    stm.setBigDecimal(1, amount);
                    stm.setString(2, fromAcc);
                    stm.executeUpdate();

                }

                String addSQL = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
                try (PreparedStatement stm = conn.prepareStatement(addSQL)) {
                    stm.setBigDecimal(1, amount);
                    stm.setString(2, toAcc);
                    stm.executeUpdate();
                }

                logTransaction(conn, fromAcc, "TRANSFER_OUT", amount, toAcc);
                logTransaction(conn, toAcc, "TRANSFER_IN", amount, fromAcc);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<String> getTransactionHistory(String accNum) throws SQLException {
        List<String> history = new ArrayList<>();
        String sql = "SELECT type, amount, target_account, created_at FROM transactions " +
                "WHERE account_number = ? ORDER BY id DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stm = conn.prepareStatement(sql)){
            stm.setString(1, accNum);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String target = rs.getString("target_account");
                String targetInfo = (target != null) ? " (Account: " + target + ")" : "";
                String entry = String.format("[%s] %s: $%.2f%s",
                        rs.getTimestamp("created_at"),
                        rs.getString("type"),
                        rs.getBigDecimal("amount"),
                        targetInfo);
                history.add(entry);
            }

        }
        return history;
    }

    public BigDecimal getBalance(String accNum) throws SQLException {
        try (Connection con = dbConfig.getConnection()) {
            return getBalanceInternal(con, accNum);

        }
    }

    public BigDecimal getBalanceInternal(Connection conn, String accNum) throws SQLException {
        String query = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            stm.setString(1, accNum);
            var rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("balance");
            } else {
                throw new IllegalArgumentException("Account not found" + accNum);
            }
        }
    }

    private void logTransaction(Connection conn, String accNum, String transactionType, BigDecimal amount, String targetAccount) throws SQLException {
        String insertTransaction = "INSERT INTO transactions (account_number, type, amount, target_account) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stm = conn.prepareStatement(insertTransaction)) {
            stm.setString(1, accNum);
            stm.setString(2, transactionType);
            stm.setBigDecimal(3, amount);
            stm.setString(4, targetAccount);
            stm.executeUpdate();
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
