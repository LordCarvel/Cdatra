package io.github.lordcarvel.cdatra.jdbc;

import io.github.lordcarvel.cdatra.example.User;
import io.github.lordcarvel.cdatra.mapping.ObjectMapper;
import io.github.lordcarvel.cdatra.mapping.ValueAnalysis;
import io.github.lordcarvel.cdatra.metadata.Analysis;
import io.github.lordcarvel.cdatra.metadata.SchemaBuilder;
import io.github.lordcarvel.cdatra.sql.SqlGenerator;
import io.github.lordcarvel.cdatra.sql.SqlQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlExecutorTest {

    private final SqlExecutor executor = new SqlExecutor();

    @Test
    void rejectsNullConnection () {

        assertThrows(IllegalArgumentException.class, () -> executor.execute(null, sql("SELECT 1")));
        assertThrows(IllegalArgumentException.class, () -> executor.query(null, sql("SELECT 1")));
        assertThrows(IllegalArgumentException.class, () -> executor.executeAndReturnGeneratedKey(null, sql("SELECT 1")));
    }

    @Test
    void rejectsNullQuery () throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            assertThrows(IllegalArgumentException.class, () -> executor.execute(connection, null));
            assertThrows(IllegalArgumentException.class, () -> executor.query(connection, null));
            assertThrows(IllegalArgumentException.class, () -> executor.executeAndReturnGeneratedKey(connection, null));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsInvalidSqlBeforeReachingTheExecutor (String text) {

        assertThrows(IllegalArgumentException.class, () -> new SqlQuery(text, List.of()));
    }

    @Test
    void bindsParametersInOrder () throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            executor.execute(connection, sql("CREATE TABLE people (id INTEGER, name VARCHAR)"));
            executor.execute(connection, new SqlQuery("INSERT INTO people VALUES (?, ?)", List.of(1, "D'Ávila")));
            executor.execute(connection, new SqlQuery("INSERT INTO people VALUES (?, ?)", Arrays.asList(2, null)));

            var byId = executor.query(connection, new SqlQuery("SELECT name FROM people WHERE id = ?", List.of(1)));
            assertEquals("D'Ávila", byId.get(0).get(0).getValue());
            var withoutName = executor.query(connection, sql("SELECT id FROM people WHERE name IS NULL"));
            assertEquals(2, withoutName.get(0).get(0).getValue());
        }
    }

    @Test
    void returnsEmptyListForSelectWithoutResults () throws Exception {

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            executor.execute(connection, sql("CREATE TABLE people (id INTEGER)"));
            assertTrue(executor.query(connection, sql("SELECT * FROM people")).isEmpty());
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

            var result = executor.query(connection, sql("SELECT * FROM people ORDER BY id"));
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

            executor.execute(connection, sql("CREATE TABLE people (original INTEGER)"));
            executor.execute(connection, sql("INSERT INTO people VALUES (1)"));
            var result = executor.query(connection, sql("SELECT original AS id FROM people"));
            assertEquals("ID", result.get(0).get(0).getColumnName());
            assertEquals(1, result.get(0).get(0).getValue());
        }
    }

    @Test
    void closesStatementWhenExecuteFails () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.execute(failingConnection(closed, false), sql("invalid")));
        assertEquals(List.of("statement"), closed);
    }

    @Test
    void closesStatementWhenQueryFailsBeforeResultSetExists () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.query(failingConnection(closed, false), sql("invalid")));
        assertEquals(List.of("statement"), closed);
    }

    @Test
    void closesResultSetAndStatementWhenReadingFails () {

        var closed = new ArrayList<String>();
        assertThrows(SQLException.class, () -> executor.query(failingConnection(closed, true), sql("SELECT 1")));
        assertEquals(List.of("resultSet", "statement"), closed);
    }

    private static SqlQuery sql (String text) {

        return new SqlQuery(text, List.of());
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

        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {

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

            if (method.getName().equals("prepareStatement")) {

                return statement;
            }

            throw new AssertionError("Executor should only prepare a statement");
        });
    }
}
