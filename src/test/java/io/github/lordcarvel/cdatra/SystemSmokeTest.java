package io.github.lordcarvel.cdatra;

import io.github.lordcarvel.cdatra.example.Main;
import io.github.lordcarvel.cdatra.jdbc.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SystemSmokeTest {

    @Test
    void runsTheActualMainAndPrintsExpectedAndOrResults () throws Exception {

        var original = System.out;
        var output = new ByteArrayOutputStream();

        try (var capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {

            System.setOut(capture);
            Main.main(new String[0]);
        } finally {

            System.setOut(original);
        }

        String text = output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String[] sections = text.split("\n\nOR:\n");
        assertEquals(2, sections.length);
        assertEquals("AND:\nID: 3 | Name: Carvel | Email: carvel2@gmail.com", sections[0]);
        assertEquals(Set.of("ID: 1 | Name: Carvel | Email: carvel@gmail.com", "ID: 4 | Name: Maria | Email: maria@gmail.com"),
                Set.copyOf(sections[1].lines().toList()));
        assertEquals(2, sections[1].lines().count());
    }

    @Test
    void opensDatabaseThroughPublicConnectionFactory () throws Exception {

        try (var connection = new DatabaseConnection().connection("jdbc:h2:mem:", "sa", "")) {

            assertFalse(connection.isClosed());
            assertTrue(connection.getAutoCommit());

            try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT 42")) {

                assertTrue(result.next());
                assertEquals(42, result.getInt(1));
            }
        }
    }

    @Test
    void propagatesUnsupportedJdbcUrl () {

        assertThrows(SQLException.class, () -> new DatabaseConnection().connection("jdbc:unknown:missing", "sa", ""));
    }
}
