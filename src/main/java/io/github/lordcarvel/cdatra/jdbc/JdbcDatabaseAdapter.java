package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.mapping.Row;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class JdbcDatabaseAdapter implements DatabaseAdapter {

    private final Connection connection;

    private final SqlExecutor sqlExecutor = new SqlExecutor();

    public JdbcDatabaseAdapter (Connection connection) {

        if (connection == null) {

            throw new IllegalArgumentException("Database connection cannot be null");
        }

        this.connection = connection;
    }

    @Override
    public void execute (String sql) throws SQLException {

        sqlExecutor.execute(connection, sql);
    }

    @Override
    public List<List<Row>> query (String sql) throws SQLException {

        return sqlExecutor.query(connection, sql);
    }

    @Override
    public Object executeAndReturnGeneratedKey (String sql) throws SQLException {

        return sqlExecutor.executeAndReturnGeneratedKey(connection, sql);
    }
}
