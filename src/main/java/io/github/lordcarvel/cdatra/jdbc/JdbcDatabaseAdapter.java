package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;

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
    public void execute (SqlQuery query) throws SQLException {

        sqlExecutor.execute(connection, query);
    }

    @Override
    public List<List<Row>> query (SqlQuery query) throws SQLException {

        return sqlExecutor.query(connection, query);
    }

    @Override
    public Object executeAndReturnGeneratedKey (SqlQuery query) throws SQLException {

        return sqlExecutor.executeAndReturnGeneratedKey(connection, query);
    }
}
