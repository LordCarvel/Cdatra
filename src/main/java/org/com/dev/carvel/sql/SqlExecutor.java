package org.com.dev.carvel.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlExecutor {
    public void execute (Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();

        statement.execute(sql);
        statement.close();
    }
}
