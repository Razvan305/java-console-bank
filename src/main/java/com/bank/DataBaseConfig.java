package com.bank;

import lombok.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class DataBaseConfig {
    private final String url;
    private final String user;
    private final String password;

    public static DataBaseConfig defaultPostgresConfig() {
        return new DataBaseConfig(
                "jdbc:postgresql://localhost:5432/bankdb",
                "bankuser",
                "bankpassword"
        );
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
