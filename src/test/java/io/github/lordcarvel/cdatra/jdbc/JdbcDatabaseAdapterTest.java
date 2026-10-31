package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.exception.CdatraException;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import io.github.lordcarvel.cdatra.sql.dialect.D1Dialect;
import io.github.lordcarvel.cdatra.sql.dialect.H2Dialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcDatabaseAdapterTest {

    private Connection connection;

    @BeforeEach
    void openDatabase () throws Exception {

        connection = DriverManager.getConnection("jdbc:h2:mem:");
    }

    @AfterEach
    void closeDatabase () throws Exception {

        connection.close();
    }

    @Test
    void usesH2DialectByDefault () {

        assertInstanceOf(H2Dialect.class, new JdbcDatabaseAdapter(connection).dialect());
    }

    @Test
    void keepsTheGivenDialect () {

        D1Dialect dialect = new D1Dialect();

        assertSame(dialect, new JdbcDatabaseAdapter(connection, dialect).dialect());
    }

    @Test
    void rejectsNullConnectionAndDialect () {

        assertThrows(IllegalArgumentException.class, () -> new JdbcDatabaseAdapter(null));
        assertThrows(IllegalArgumentException.class, () -> new JdbcDatabaseAdapter(null, new H2Dialect()));
        assertThrows(IllegalArgumentException.class, () -> new JdbcDatabaseAdapter(connection, null));
    }

    @Test
    void executeTranslatesDatabaseErrorsKeepingTheCause () {

        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(connection);

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.execute(invalidQuery()));

        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void queryTranslatesDatabaseErrorsKeepingTheCause () {

        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(connection);

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.query(invalidQuery()));

        assertInstanceOf(SQLException.class, error.getCause());
    }

    @Test
    void executeAndReturnGeneratedKeyTranslatesDatabaseErrorsKeepingTheCause () {

        JdbcDatabaseAdapter adapter = new JdbcDatabaseAdapter(connection);

        CdatraException error = assertThrows(CdatraException.class, () -> adapter.executeAndReturnGeneratedKey(invalidQuery()));

        assertInstanceOf(SQLException.class, error.getCause());
    }

    private SqlQuery invalidQuery () {

        return new SqlQuery("not SQL", List.of());
    }
}
