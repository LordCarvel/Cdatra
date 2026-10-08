package org.com.dev.carvel.sql;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.schemaBuilder.SchemaBuilder;
import org.com.dev.carvel.table.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlGeneratorTest {

    private final SqlGenerator generator = new SqlGenerator();

    @ParameterizedTest
    @MethodSource("invalidTables")
    void validatesTablesAtSqlGenerationRatherThanSchemaBuilding (Table table) {

        assertThrows(IllegalArgumentException.class, () -> generator.createTable(table));
        assertThrows(IllegalArgumentException.class, () -> generator.insert(table, List.of(new Row("id", 1))));
    }

    static Stream<Table> invalidTables () {

        var builder = new SchemaBuilder();
        var columns = List.of(new ColumnDefinition("id", int.class));

        return Stream.of(null, builder.build(null, columns), builder.build(" ", columns),
                builder.build("people", null), builder.build("people", List.of()),
                builder.build("people", Arrays.asList((ColumnDefinition) null)),
                builder.build("people", List.of(new ColumnDefinition(null, int.class))),
                builder.build("people", List.of(new ColumnDefinition(" ", int.class))),
                builder.build("people", List.of(new ColumnDefinition("id", null))),
                builder.build("people", List.of(new ColumnDefinition("id", Object.class))),
                builder.build("people", List.of(new ColumnDefinition("id", int.class), new ColumnDefinition("ID", int.class))));
    }

    @Test
    void buildsCreateTableForSupportedTypes () {

        var table = new Table("people", List.of(new ColumnDefinition("id", int.class),
                new ColumnDefinition("name", String.class), new ColumnDefinition("total", Long.class),
                new ColumnDefinition("active", boolean.class), new ColumnDefinition("score", Double.class)));
        assertEquals("CREATE TABLE people (id INTEGER, name VARCHAR, total BIGINT, active BOOLEAN, score DOUBLE);", generator.createTable(table));
    }

    @Test
    void insertsEscapedTextNullAndSupportedValues () {

        var table = new Table("people", List.of(new ColumnDefinition("id", int.class),
                new ColumnDefinition("name", String.class), new ColumnDefinition("optional", String.class),
                new ColumnDefinition("total", long.class), new ColumnDefinition("active", boolean.class),
                new ColumnDefinition("score", double.class)));
        assertEquals("INSERT INTO people (id, name, optional, total, active, score) VALUES (1, 'D''Ávila', NULL, 5, true, 1.5);",
                generator.insert(table, List.of(new Row("id", 1), new Row("name", "D'Ávila"),
                        new Row("optional", null), new Row("total", 5L), new Row("active", true), new Row("score", 1.5))));
    }

    @ParameterizedTest
    @MethodSource("invalidRows")
    void rejectsInvalidInsertRows (List<Row> rows) {

        var table = new Table("people", List.of(new ColumnDefinition("id", int.class)));
        assertThrows(IllegalArgumentException.class, () -> generator.insert(table, rows));
    }

    static Stream<List<Row>> invalidRows () {

        return Stream.of(null, List.of(), Arrays.asList((Row) null), List.of(new Row(null, 1)),
                List.of(new Row(" ", 1)), List.of(new Row("id", 1), new Row("ID", 2)),
                List.of(new Row("unknown", 1)), List.of(new Row("id", "wrong type")));
    }
}
