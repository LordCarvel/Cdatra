package org.com.dev.carvel.audit;

import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.analysis.ValueAnalysis;
import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.annotations.GeneratedValue;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.mapper.ObjectMapper;
import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.query.QueryCondition;
import org.com.dev.carvel.query.QueryFilter;
import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.sql.SqlExecutor;
import org.com.dev.carvel.sql.SqlGenerator;
import org.com.dev.carvel.support.TestEntities;
import org.com.dev.carvel.table.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Regression contracts for the defects found during the release audit; always executed.
class KnownDefectsTest {

    private final SqlGenerator generator = new SqlGenerator();

    @Test
    void D01_updateEscapesAnApostropheInTextPrimaryKey () throws Exception {

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var repository = new Repository<>(TestEntities.TextRecord.class, connection);
            repository.createTable();
            var record = new TestEntities.TextRecord("O'Brien", "original");
            repository.save(record);
            record.label = "updated";
            assertDoesNotThrow(() -> repository.update(record));
            assertEquals("updated", repository.findById(record.id).label);
            repository.delete(record);
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Test
    void D01_textKeyCannotBroadenAnUpdateToOtherRecords () throws Exception {

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var repository = new Repository<>(TestEntities.TextRecord.class, connection);
            repository.createTable();
            var attack = new TestEntities.TextRecord("x' OR '1'='1", "original");
            repository.save(attack);
            repository.save(new TestEntities.TextRecord("victim", "untouched"));
            attack.label = "changed";
            repository.update(attack);
            assertEquals("changed", repository.findById(attack.id).label);
            assertEquals("untouched", repository.findById("victim").label);
            repository.delete(attack);
            assertEquals(1, repository.findAll().size());
            assertEquals("untouched", repository.findById("victim").label);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null-id", "null-row", "unknown-column", "null-column", "blank-column", "duplicate-column", "wrong-type", "wrong-id-type", "primary-update"})
    void D02_updateRejectsInvalidInputsBeforeGeneratingSql (String scenario) {

        var table = table();
        var id = new Row("id", 1);
        List<Row> rows = List.of(new Row("label", "new"));

        if (scenario.equals("null-id")) {

            id = new Row("id", null);
        } else if (scenario.equals("null-row")) {

            rows = Arrays.asList((Row) null);
        } else if (scenario.equals("unknown-column")) {

            rows = List.of(new Row("unknown", "new"));
        } else if (scenario.equals("null-column")) {

            rows = List.of(new Row(null, "new"));
        } else if (scenario.equals("blank-column")) {

            rows = List.of(new Row(" ", "new"));
        } else if (scenario.equals("duplicate-column")) {

            rows = List.of(new Row("label", "a"), new Row("LABEL", "b"));
        } else if (scenario.equals("wrong-id-type")) {

            id = new Row("id", "1");
        } else if (scenario.equals("primary-update")) {

            id = new Row("label", "original");
            rows = List.of(new Row("id", 2));
        } else {

            rows = List.of(new Row("label", 42));
        }

        Row selectedId = id;
        List<Row> selectedRows = rows;
        assertThrows(IllegalArgumentException.class, () -> generator.update(table, selectedRows, selectedId));
    }

    @Test
    void D03_deleteCannotBroadenItsTargetThroughAnUncheckedIdentifier () throws Exception {

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var executor = new SqlExecutor();
            executor.execute(connection, generator.createTable(table()));
            executor.execute(connection, "INSERT INTO people VALUES (1, 'first'), (2, 'second')");
            assertThrows(IllegalArgumentException.class, () -> generator.delete(table(), new Row("id = 1 OR id", 2)));
            assertEquals(2, executor.query(connection, "SELECT * FROM people").size());
        }
    }

    @Test
    void D04_savesAnEntityContainingOnlyAnIdentityColumn () throws Exception {

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var repository = new Repository<>(TestEntities.OnlyId.class, connection);
            repository.createTable();
            var entity = new TestEntities.OnlyId();
            assertDoesNotThrow(() -> repository.save(entity));
            assertTrue(entity.id > 0);
            assertNotNull(repository.findById(entity.id));
            repository.update(entity);
            assertNotNull(repository.findById(entity.id));
            repository.delete(entity);
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Test
    void D05_includesInheritedAnnotatedId () throws Exception {

        assertEquals("id", assertDoesNotThrow(() -> new Analysis().analyzeIdColumnName(TestEntities.Child.class)));

        try (var connection = DriverManager.getConnection("jdbc:h2:mem:")) {

            var repository = new Repository<>(TestEntities.Child.class, connection);
            repository.createTable();
            var entity = new TestEntities.Child();
            entity.id = 7;
            entity.label = "inherited";
            repository.save(entity);
            assertEquals(7, repository.findById(7).id);
            assertEquals("inherited", repository.findById(7).label);
            entity.label = "updated";
            repository.update(entity);
            assertEquals("updated", repository.findById(7).label);
            repository.delete(entity);
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Test
    void D06_ignoresStaticAnnotatedFieldsWhenBuildingAnEntity () throws Exception {

        var columns = new Analysis().analize(StaticColumn.class);
        assertEquals(List.of("label"), columns.stream().map(ColumnDefinition::getName).toList());
        assertEquals(List.of("label"), new ValueAnalysis().analyze(new StaticColumn()).stream().map(Row::getColumnName).toList());
        var entity = (StaticColumn) new ObjectMapper().map(List.of(new Row("label", "mapped"), new Row("global", "unexpected")), StaticColumn.class);
        assertEquals("mapped", entity.label);
        assertEquals("shared", StaticColumn.global);
    }

    @Test
    void D07_rejectsDuplicateResultColumnLabelsInsteadOfSilentlyOverwriting () {

        var rows = List.of(new Row("id", 1), new Row("ID", 2), new Row("label", "value"));
        assertThrows(IllegalArgumentException.class, () -> new ObjectMapper().map(rows, TestEntities.ManualRecord.class));
    }

    @Test
    void D08_rejectsGeneratedValueWithoutColumnAnnotation () {

        assertThrows(IllegalArgumentException.class, () -> new Analysis().analize(GeneratedWithoutColumn.class));
    }

    @Test
    void D09_generatedKeyExecutionValidatesNullConnection () {

        assertThrows(IllegalArgumentException.class, () -> new SqlExecutor().executeAndReturnGeneratedKey(null, "INSERT INTO people VALUES (1)"));
    }

    @ParameterizedTest
    @CsvSource({"select, null-name", "select, blank-name", "select, null-columns", "select, null-column", "update, null-name", "delete, blank-name",
            "select, injected-name", "update, injected-name", "delete, injected-name", "select, injected-column", "update, injected-column", "delete, injected-column",
            "select, empty-columns", "update, empty-columns", "delete, empty-columns"})
    void D10_validatesTableMetadataConsistentlyAcrossOperations (String operation, String scenario) {

        String name = scenario.equals("null-name") ? null : scenario.equals("blank-name") ? " " : "people";
        name = scenario.equals("injected-name") ? "people; DROP TABLE people; --" : name;
        List<ColumnDefinition> columns = scenario.equals("null-columns") ? null :
                scenario.equals("null-column") ? Arrays.asList((ColumnDefinition) null) : table().getColumnDefinitions();
        columns = scenario.equals("injected-column") ? List.of(new ColumnDefinition("id = 1 OR id", int.class, true, false)) : columns;
        columns = scenario.equals("empty-columns") ? List.of() : columns;
        var invalid = new Table(name, columns);
        assertThrows(IllegalArgumentException.class, () -> {

            if (operation.equals("select")) {

                generator.selectBy(invalid, new Row("id", 1), Operator.EQUAL);
            } else if (operation.equals("update")) {

                generator.update(invalid, List.of(new Row("label", "new")), new Row("id", 1));
            } else {

                generator.delete(invalid, new Row("id", 1));
            }
        });
    }

    private Table table () {

        return new Table("people", List.of(new ColumnDefinition("id", int.class, true, false),
                new ColumnDefinition("label", String.class, false, false)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void D11_rejectsNonFiniteNumbersInsteadOfProducingInvalidSql (double value) {

        var table = new Table("numbers", List.of(new ColumnDefinition("id", int.class, true, false), new ColumnDefinition("score", double.class, false, false)));
        assertThrows(IllegalArgumentException.class, () -> generator.insert(table, List.of(new Row("score", value))));
        assertThrows(IllegalArgumentException.class, () -> generator.update(table, List.of(new Row("score", value)), new Row("id", 1)));
        assertThrows(IllegalArgumentException.class, () -> generator.selectBy(table, new Row("score", value), Operator.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> generator.selectById(table, new Row("score", value)));
        assertThrows(IllegalArgumentException.class, () -> generator.delete(table, new Row("score", value)));
        var condition = new QueryCondition("score", value, Operator.EQUAL);
        assertThrows(IllegalArgumentException.class, () -> generator.selectByConditions(table, List.of(condition)));
        assertThrows(IllegalArgumentException.class, () -> generator.selectByFilters(table, List.of(new QueryFilter(condition, null))));
    }

    public static class StaticColumn {

        @Column(columName = "global") public static String global = "shared";
        @Column(columName = "label") public String label;
    }

    public static class GeneratedWithoutColumn {

        @GeneratedValue public int id;
    }
}
