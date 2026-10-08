package io.github.lordcarvel.cdatra.metadata;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.Entity;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;
import io.github.lordcarvel.cdatra.mapping.ValueAnalysis;
import io.github.lordcarvel.cdatra.support.TestEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EntityMetadataTest {

    private final Analysis analysis = new Analysis();
    private final ValueAnalysis values = new ValueAnalysis();

    @Test
    void readsTableNameAndIdMetadata () {

        var columns = analysis.analize(TestEntities.Record.class);
        assertEquals("records", analysis.analyzeTableName(TestEntities.Record.class));
        assertEquals("id", analysis.analyzeIdColumnName(TestEntities.Record.class));
        assertEquals(7, columns.size());
        var id = columns.stream().filter(column -> column.getName().equals("id")).findFirst().orElseThrow();
        assertTrue(id.isId());
        assertTrue(id.isGeneratedValue());
        assertEquals(int.class, id.getType());
        assertTrue(columns.stream().filter(column -> !column.getName().equals("id")).noneMatch(column -> column.isId() || column.isGeneratedValue()));
    }

    @Test
    void skipsGeneratedIdButIncludesManualId () throws Exception {

        var entity = new TestEntities.Record("Carvel", null, 7, true, 1.5);
        entity.id = 99;
        entity.ignored = "ignore me";
        var rows = values.analyze(entity);
        assertEquals(6, rows.size());
        assertTrue(rows.stream().noneMatch(row -> row.getColumnName().equals("id") || row.getColumnName().equals("ignored")));
        assertEquals(99, values.analyzeId(entity).getValue());

        var manual = new TestEntities.ManualRecord(42, "manual");
        assertEquals(2, values.analyze(manual).size());
        assertEquals(42, values.analyzeId(manual).getValue());
    }

    @Test
    void writesGeneratedValueAndLeavesManualIdsAlone () throws Exception {

        var generated = new TestEntities.Record();
        values.setGeneratedValue(generated, 12);
        assertEquals(12, generated.id);

        var manual = new TestEntities.ManualRecord(42, "manual");
        values.setGeneratedValue(manual, 12);
        assertEquals(42, manual.id);
    }

    @Test
    void rejectsNullValuesAndIncompatibleGeneratedKeys () {

        assertThrows(IllegalArgumentException.class, () -> values.setGeneratedValue(null, 1));
        assertThrows(IllegalArgumentException.class, () -> values.setGeneratedValue(new TestEntities.Record(), "one"));
        assertThrows(IllegalArgumentException.class, () -> values.setGeneratedValue(new TestEntities.Record(), null));
        assertThrows(IllegalArgumentException.class, () -> values.analyzeId(null));
        assertThrows(IllegalArgumentException.class, () -> analysis.analyzeIdColumnName(null));
        assertThrows(IllegalArgumentException.class, () -> analysis.analyzeTableName(null));
        assertThrows(IllegalArgumentException.class, () -> analysis.analyzeTableName(String.class));
    }

    static Stream<Class<?>> invalidIds () {

        return Stream.of(IdWithoutColumn.class, TwoIds.class, GeneratedWithoutId.class);
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    void rejectsInvalidIdDeclarations (Class<?> type) throws Exception {

        assertThrows(IllegalArgumentException.class, () -> analysis.analize(type));
        assertThrows(IllegalArgumentException.class, () -> values.analyze(type.getDeclaredConstructor().newInstance()));
    }

    @Test
    void explainsMissingIdAndMissingIdColumn () {

        assertThrows(IllegalArgumentException.class, () -> analysis.analyzeIdColumnName(NoId.class));
        assertThrows(IllegalArgumentException.class, () -> values.analyzeId(new NoId()));
        assertThrows(IllegalArgumentException.class, () -> analysis.analyzeIdColumnName(IdWithoutColumn.class));
        assertThrows(IllegalArgumentException.class, () -> values.analyzeId(new IdWithoutColumn()));
    }

    @Test
    void supportsAnIdWithoutGeneratedValue () {

        var id = analysis.analize(TestEntities.ManualRecord.class).get(0);
        assertTrue(id.isId());
        assertFalse(id.isGeneratedValue());
    }

    @Entity(tableNaame = "no_id")
    public static class NoId {

        @Column(columName = "name") public String name;
    }

    public static class IdWithoutColumn {

        @Id public int id;
    }

    public static class TwoIds {

        @Id @Column(columName = "first") public int first;
        @Id @Column(columName = "second") public int second;
    }

    public static class GeneratedWithoutId {

        @GeneratedValue @Column(columName = "name") public int name;
    }
}
