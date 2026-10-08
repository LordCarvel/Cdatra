package org.com.dev.carvel.sql;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.query.LogicalOperator;
import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.query.QueryCondition;
import org.com.dev.carvel.query.QueryFilter;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.table.Table;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlQueryTest {

    private final SqlGenerator generator = new SqlGenerator();
    private final Table table = new Table("people", List.of(
            new ColumnDefinition("id", int.class, true, true),
            new ColumnDefinition("name", String.class, false, false)));

    @ParameterizedTest
    @CsvSource({"EQUAL, =", "NOT_EQUAL, !=", "GREATER_THAN, >", "LESS_THAN, <", "GREATER_THAN_OR_EQUAL, >=", "LESS_THAN_OR_EQUAL, <="})
    void rendersEveryComparison (Operator operator, String sqlOperator) {

        assertEquals("SELECT * FROM people WHERE id " + sqlOperator + " 2;", generator.selectBy(table, new Row("id", 2), operator));
    }

    @Test
    void rendersNullEqualityAndInequality () {

        assertEquals("SELECT * FROM people WHERE name IS NULL;", generator.selectBy(table, new Row("name", null), Operator.EQUAL));
        assertEquals("SELECT * FROM people WHERE name IS NOT NULL;", generator.selectBy(table, new Row("name", null), Operator.NOT_EQUAL));
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = {"GREATER_THAN", "LESS_THAN", "GREATER_THAN_OR_EQUAL", "LESS_THAN_OR_EQUAL"})
    void rejectsOrderedComparisonWithNull (Operator operator) {

        assertThrows(IllegalArgumentException.class, () -> generator.selectBy(table, new Row("name", null), operator));
    }

    @Test
    void preservesSqlLookingTextAndEscapesQuotes () {

        String value = "D'Ávila; SELECT * FROM people WHERE 'x'='x' --";
        assertEquals("SELECT * FROM people WHERE name = 'D''Ávila; SELECT * FROM people WHERE ''x''=''x'' --';",
                generator.selectBy(table, new Row("name", value), Operator.EQUAL));
        assertEquals("SELECT * FROM people WHERE name = 'D''Ávila; SELECT * FROM people WHERE ''x''=''x'' --' AND id > 1;",
                generator.selectByConditions(table, List.of(new QueryCondition("name", value, Operator.EQUAL),
                        new QueryCondition("id", 1, Operator.GREATER_THAN))));
    }

    @Test
    void combinesConditionsAndLogicalFiltersWithoutMutatingInputs () {

        var conditions = List.of(new QueryCondition("id", 1, Operator.GREATER_THAN), new QueryCondition("name", "Carvel", Operator.EQUAL));
        assertEquals("SELECT * FROM people WHERE id > 1 AND name = 'Carvel';", generator.selectByConditions(table, conditions));
        assertEquals("SELECT * FROM people WHERE id > 1;", generator.selectByConditions(table, List.of(conditions.get(0))));
        var filters = List.of(new QueryFilter(conditions.get(0), null), new QueryFilter(conditions.get(1), LogicalOperator.OR));
        assertEquals("SELECT * FROM people WHERE id > 1 OR name = 'Carvel';", generator.selectByFilters(table, filters));
        assertEquals("SELECT * FROM people WHERE id > 1 AND name = 'Carvel';",
                generator.selectByFilters(table, List.of(filters.get(0), new QueryFilter(conditions.get(1), LogicalOperator.AND))));
        assertEquals(2, conditions.size());
        assertNull(filters.get(0).getLogicalOperator());
        assertEquals("Carvel", conditions.get(1).getValue());
    }

    @Test
    void ignoresFirstLogicalOperatorAndPreservesMixedSqlPrecedence () {

        var first = new QueryCondition("id", 1, Operator.EQUAL);
        assertEquals("SELECT * FROM people WHERE id = 1;", generator.selectByFilters(table, List.of(new QueryFilter(first, LogicalOperator.OR))));
        assertEquals("SELECT * FROM people WHERE id = 1 OR name = 'Carvel' AND id > 2;",
                generator.selectByFilters(table, List.of(new QueryFilter(first, null),
                        new QueryFilter(new QueryCondition("name", "Carvel", Operator.EQUAL), LogicalOperator.OR),
                        new QueryFilter(new QueryCondition("id", 2, Operator.GREATER_THAN), LogicalOperator.AND))));
    }

    @Test
    void rendersSelectAllAndIdSelection () {

        assertEquals("SELECT * FROM people;", generator.selectAll(table));
        assertEquals("SELECT * FROM people WHERE id = 7;", generator.selectById(table, new Row("id", 7)));
        assertEquals("SELECT * FROM people WHERE name = 'O''Brien';", generator.selectById(table, new Row("name", "O'Brien")));
    }

    @TestFactory
    Stream<DynamicTest> rejectsInvalidQueryInputs () {

        var condition = new QueryCondition("id", 1, Operator.EQUAL);
        var invalid = List.<org.junit.jupiter.api.function.Executable>of(
                () -> generator.selectAll(null),
                () -> generator.selectAll(new Table(null, table.getColumnDefinitions())),
                () -> generator.selectAll(new Table(" ", table.getColumnDefinitions())),
                () -> generator.selectBy(null, new Row("id", 1), Operator.EQUAL),
                () -> generator.selectBy(table, null, Operator.EQUAL),
                () -> generator.selectBy(table, new Row(null, 1), Operator.EQUAL),
                () -> generator.selectBy(table, new Row(" ", 1), Operator.EQUAL),
                () -> generator.selectBy(table, new Row("missing", 1), Operator.EQUAL),
                () -> generator.selectBy(table, new Row("id", 1), null),
                () -> generator.selectBy(table, new Row("id", new Object()), Operator.EQUAL),
                () -> generator.selectById(null, new Row("id", 1)),
                () -> generator.selectById(table, null),
                () -> generator.selectById(table, new Row(null, 1)),
                () -> generator.selectById(table, new Row(" ", 1)),
                () -> generator.selectById(table, new Row("id", null)),
                () -> generator.selectById(table, new Row("missing", 1)),
                () -> generator.selectByConditions(null, List.of(condition)),
                () -> generator.selectByConditions(table, null),
                () -> generator.selectByConditions(table, List.of()),
                () -> generator.selectByConditions(table, Arrays.asList((QueryCondition) null)),
                () -> generator.selectByFilters(null, List.of(new QueryFilter(condition, null))),
                () -> generator.selectByFilters(table, null),
                () -> generator.selectByFilters(table, List.of()),
                () -> generator.selectByFilters(table, Arrays.asList((QueryFilter) null)),
                () -> generator.selectByFilters(table, List.of(new QueryFilter(null, null))),
                () -> generator.selectByFilters(table, List.of(new QueryFilter(condition, null), new QueryFilter(condition, null))));

        return java.util.stream.IntStream.range(0, invalid.size())
                .mapToObj(i -> DynamicTest.dynamicTest("invalid-query-" + (i + 1), () -> assertThrows(IllegalArgumentException.class, invalid.get(i))));
    }

    @Test
    void rendersUpdateDeleteAndIdentitySchema () {

        assertEquals("CREATE TABLE people (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR);", generator.createTable(table));
        assertEquals("UPDATE people SET name = 'O''Brien' WHERE id = 7;", generator.update(table, List.of(new Row("name", "O'Brien")), new Row("id", 7)));
        assertEquals("UPDATE people SET name = NULL WHERE id = 7;", generator.update(table, List.of(new Row("name", null)), new Row("id", 7)));
        assertEquals("DELETE FROM people WHERE id = 7;", generator.delete(table, new Row("id", 7)));
        assertEquals("DELETE FROM people WHERE name = 'O''Brien';", generator.delete(table, new Row("name", "O'Brien")));
    }

    @TestFactory
    Stream<DynamicTest> rejectsInvalidWriteInputsAlreadyValidatedByTheApi () {

        var rows = List.of(new Row("name", "Carvel"));
        var id = new Row("id", 1);
        var invalid = List.<org.junit.jupiter.api.function.Executable>of(
                () -> generator.update(null, rows, id),
                () -> generator.update(table, null, id),
                () -> generator.update(table, List.of(), id),
                () -> generator.update(table, rows, null),
                () -> generator.update(table, rows, new Row(null, 1)),
                () -> generator.update(table, rows, new Row(" ", 1)),
                () -> generator.update(table, rows, new Row("missing", 1)),
                () -> generator.update(table, List.of(new Row("ID", 2)), id),
                () -> generator.delete(null, id),
                () -> generator.delete(table, null),
                () -> generator.delete(table, new Row("id", null)));

        return java.util.stream.IntStream.range(0, invalid.size())
                .mapToObj(i -> DynamicTest.dynamicTest("invalid-write-" + (i + 1), () -> assertThrows(IllegalArgumentException.class, invalid.get(i))));
    }
}
