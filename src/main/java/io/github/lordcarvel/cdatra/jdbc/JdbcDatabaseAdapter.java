package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.adapter.DatabaseAdapter;
import io.github.lordcarvel.cdatra.exception.CdatraException;
import io.github.lordcarvel.cdatra.mapping.Row;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.H2Dialect;
import io.github.lordcarvel.cdatra.sql.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class JdbcDatabaseAdapter implements DatabaseAdapter {

    private final Connection connection;

    private final SqlExecutor sqlExecutor = new SqlExecutor();

    private final SqlDialect dialect;

    public JdbcDatabaseAdapter (Connection connection) {

        this(connection, new H2Dialect());
    }

    public JdbcDatabaseAdapter (Connection connection, SqlDialect dialect) {

        if (connection == null) {

            throw new IllegalArgumentException("Database connection cannot be null");
        }

        if (dialect == null) {

            throw new IllegalArgumentException("SQL dialect cannot be null");
        }

        this.connection = connection;
        this.dialect = dialect;
    }

    @Override
    public void execute (SqlQuery query) {

        try {

            sqlExecutor.execute(connection, query);
        } catch (SQLException e) {

            throw new CdatraException("Failed to execute query", e);
        }
    }

    @Override
    public List<List<Row>> query (SqlQuery query) {

        try {

            return sqlExecutor.query(connection, query);
        } catch (SQLException e) {

            throw new CdatraException("Failed to execute query", e);
        }
    }

    @Override
    public Object executeAndReturnGeneratedKey (SqlQuery query) {

        try {

            return sqlExecutor.executeAndReturnGeneratedKey(connection, query);
        } catch (SQLException e) {

            throw new CdatraException("Failed to execute query", e);
        }
    }

    @Override
    public SqlDialect dialect () {

        return dialect;
    }
}
