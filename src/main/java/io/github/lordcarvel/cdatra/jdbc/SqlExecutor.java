package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;

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

    public void execute (Connection connection, SqlQuery query) throws SQLException {

        validate(connection, query);

        try (PreparedStatement statement = connection.prepareStatement(query.getSql())) {

            bindParameters(statement, query.getParameters());

            statement.execute();
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

                for (int i = 1; i <= metadata.getColumnCount(); ) {

                    rows.add(new Row(metadata.getColumnLabel(i), resultSet.getObject(i)));

                    i = i + 1;
                }

                lists.add(rows);
            }
        }

        return lists;
    }

    public List<List<Row>> query (Connection connection, SqlQuery query) throws SQLException {

        validate(connection, query);
        List<List<Row>> lists = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query.getSql())) {

            bindParameters(statement, query.getParameters());

            try (ResultSet resultSet = statement.executeQuery()) {

                ResultSetMetaData metadata = resultSet.getMetaData();

                while (resultSet.next()) {

                    List<Row> rows = new ArrayList<>();

                    for (int i = 1; i <= metadata.getColumnCount(); ) {

                        rows.add(new Row(metadata.getColumnLabel(i), resultSet.getObject(i)));

                        i = i + 1;
                    }

                    lists.add(rows);
                }
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

    private void validate (Connection connection, SqlQuery query) {

        if (connection == null) {

            throw new IllegalArgumentException("Database connection cannot be null");
        }

        if (query == null) {

            throw new IllegalArgumentException("SQL query cannot be null");
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

    public Object executeAndReturnGeneratedKey (Connection connection, SqlQuery query) throws SQLException {

        validate(connection, query);

        try (PreparedStatement statement = connection.prepareStatement(query.getSql(), Statement.RETURN_GENERATED_KEYS)) {

            bindParameters(statement, query.getParameters());

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    return resultSet.getObject(1);
                }
            }
        }

        return null;
    }

    private void bindParameters (PreparedStatement statement, List<Object> parameters) throws SQLException {

        for (int i = 0; i < parameters.size(); ) {

            statement.setObject(i + 1, parameters.get(i));

            i = i + 1;
        }
    }
}
