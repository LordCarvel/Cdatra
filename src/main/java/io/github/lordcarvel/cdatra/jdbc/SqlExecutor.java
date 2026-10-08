package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.mapping.Row;

import java.sql.*;
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

    private void validate (Connection connection, String sql) {

        if (connection == null) {

            throw new IllegalArgumentException("Database connection cannot be null");
        }

        if (sql == null || sql.isBlank()) {

            throw new IllegalArgumentException("SQL cannot be null or blank");
        }
    }

    public Object executeAndReturnGeneratedKey (Connection connection, String sql) throws SQLException {

        validate(connection, sql);

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    return resultSet.getObject(1);
                }
            }
        }

        return null;
    }
}
