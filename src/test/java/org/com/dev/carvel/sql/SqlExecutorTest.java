package org.com.dev.carvel.sql;

import org.com.dev.carvel.DatabaseConnection;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlExecutorTest {

    private final SqlExecutor executor = new SqlExecutor();

    @Test
    void rejectsNullConnection () {

        assertThrows(IllegalArgumentException.class, () -> executor.execute(null, "SELECT 1"));
        assertThrows(IllegalArgumentException.class, () -> executor.query(null, "SELECT 1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsInvalidSql (String sql) throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            assertThrows(IllegalArgumentException.class, () -> executor.execute(connection, sql));
            assertThrows(IllegalArgumentException.class, () -> executor.query(connection, sql));
        }
    }

    @Test
    void returnsEmptyListForSelectWithoutResults () throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            executor.execute(connection, "CREATE TABLE people (id INTEGER)");
            assertTrue(executor.query(connection, "SELECT * FROM people").isEmpty());
            assertFalse(connection.isClosed());
        }
    }

    @Test
    void createsInsertsQueriesAndMapsMultipleEntities () throws Exception {

        var generator = new SqlGenerator();
        var values = new ValueAnalysis();
        var mapper = new ObjectMapper();
        var table = new SchemaBuilder().build("people", new Analysis().analize(User.class));

        try (Connection connection = new DatabaseConnection().connection("jdbc:h2:mem:", "sa", "")) {

            executor.execute(connection, generator.createTable(table));
            executor.execute(connection, generator.insert(table, values.analyze(new User(1, "D'Ávila", "first@example.com", null, "ignored"))));
            executor.execute(connection, generator.insert(table, values.analyze(new User(2, "Carvel", null, null, "ignored"))));

            var result = executor.query(connection, "SELECT * FROM people ORDER BY id");
            assertEquals(2, result.size());
            assertEquals(3, result.get(0).size());
            assertEquals(3, result.get(1).size());

            var first = (User) mapper.map(result.get(0), User.class);
            var second = (User) mapper.map(result.get(1), User.class);

            assertEquals(1, first.getId());
            assertEquals("D'Ávila", first.getName());
            assertEquals("first@example.com", first.getEmail());
            assertEquals(2, second.getId());
            assertEquals("Carvel", second.getName());
            assertNull(second.getEmail());
            assertNull(first.getTemporary());
            assertFalse(connection.isClosed());
        }
    }

    @Test
    void preservesSelectAliasesAsColumnNames () throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            executor.execute(connection, "CREATE TABLE people (original INTEGER)");
            executor.execute(connection, "INSERT INTO people VALUES (1)");
            var result = executor.query(connection, "SELECT original AS id FROM people");
            assertEquals("ID", result.get(0).get(0).getColumnName());
            assertEquals(1, result.get(0).get(0).getValue());
        }
    }

    @Test
    void closesStatementWhenExecuteFails () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.execute(failingConnection(closed, false), "invalid"));
        assertEquals(List.of("statement"), closed);
    }

    @Test
    void closesStatementWhenQueryFailsBeforeResultSetExists () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.query(failingConnection(closed, false), "invalid"));
        assertEquals(List.of("statement"), closed);
    }

    @Test
    void closesResultSetAndStatementWhenReadingFails () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.query(failingConnection(closed, true), "SELECT 1"));
        assertEquals(List.of("resultSet", "statement"), closed);
    }

    // Small JDBC doubles to force failures that a normal H2 query rarely produces.
    private Connection failingConnection (List<String> closed, boolean resultCreated) {

        ResultSet result = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {

            if (method.getName().equals("close")) {

                closed.add("resultSet");

                return null;
            }

            throw new SQLException("Reading failed");
        });

        Statement statement = (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Statement.class}, (proxy, method, args) -> {

            if (method.getName().equals("close")) {

                closed.add("statement");

                return null;
            }

            if (method.getName().equals("executeQuery") && resultCreated) {

                return result;
            }

            throw new SQLException("Execution failed");
        });

        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {

            if (method.getName().equals("createStatement")) {

                return statement;
            }

            throw new AssertionError("Executor should only create a statement");
        });
    }
}
