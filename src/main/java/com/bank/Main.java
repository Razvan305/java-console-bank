package com.bank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        BankService bankService = new BankService(DataBaseConfig.defaultPostgresConfig());
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== POSTGRES CONSOLE BANK ===");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Check Balance & History");
            System.out.println("6. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            if (choice.equals("6")) {
                System.out.println("Exiting...");
                break;
            }

            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Enter account number: ");
                        String accNum = scanner.nextLine();
                        System.out.print("Enter holder name: ");
                        String holderName = scanner.nextLine();
                        System.out.print("Enter initial deposit: ");
                        BigDecimal initialDeposit = new BigDecimal(scanner.nextLine());
                        bankService.createAccount(accNum, holderName, initialDeposit);
                        System.out.println("Account created successfully.");
                    }
                    case "2" -> {
                        System.out.print("Enter account number: ");
                        String accNum = scanner.nextLine();
                        System.out.print("Enter deposit amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());
                        bankService.deposit(accNum, amount);
                        System.out.println("Deposit successful.");
                    }
                    case "3" -> {
                        System.out.print("Enter account number: ");
                        String accNum = scanner.nextLine();
                        System.out.print("Enter deposit amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());
                        bankService.withdraw(accNum, amount);
                        System.out.println("Withdrawal successful.");
                    }
                    case "4" -> {
                        System.out.print("From account: ");
                        String from = scanner.nextLine();
                        System.out.print("To account: ");
                        String to = scanner.nextLine();
                        System.out.print("Ammount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());
                        bankService.trasnfer(from, to, amount);
                        System.out.println("Transfer completed.");
                    }
                    case "5" -> {
                        System.out.print("Account number: ");
                        String accNum = scanner.nextLine();
                        System.out.printf("Current Balance: $%.2f%n", bankService.getBalance(accNum));
                        System.out.println("--- Transaction History ---");
                        List<String> history = bankService.getTransactionHistory(accNum);
                        if (history.isEmpty()) {
                            System.out.println("No transactions found");
                        } else {
                            history.forEach(System.out::println);
                        }
                    }

                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }
}