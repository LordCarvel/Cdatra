package org.com.dev.carvel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public Connection connection (String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}