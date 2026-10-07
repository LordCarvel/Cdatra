package org.com.dev.carvel.sql;

import org.com.dev.carvel.row.Row;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlExecutor {
    public void execute (Connection connection, String sql) throws SQLException {
        validate(connection, sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public List<List<Row>> query (Connection connection, String sql) throws SQLException {
        validate(connection, sql);
        List<List<Row>> lists = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            ResultSetMetaData metadata = resultSet.getMetaData();

            while (resultSet.next()) {
                List<Row> rows = new ArrayList<>();

                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    rows.add(new Row(metadata.getColumnLabel(i), resultSet.getObject(i)));
                }

                lists.add(rows);
            }

        }

        return lists;
    }

    private void validate(Connection connection, String sql) {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection cannot be null");
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL cannot be null or blank");
        }
    }
}
