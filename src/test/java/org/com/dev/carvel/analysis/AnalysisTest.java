package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.row.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisTest {
    private final Analysis analysis = new Analysis();
    private final ValueAnalysis values = new ValueAnalysis();

    @Test
    void rejectsNullTypeAndObject() {
        assertThrows(IllegalArgumentException.class, () -> analysis.analize(null));
        assertThrows(IllegalArgumentException.class, () -> values.analyze(null));
    }

    @Test
    void allowsEntitiesWithoutColumns() throws Exception {
        assertTrue(analysis.analize(Empty.class).isEmpty());
        assertTrue(values.analyze(new Empty()).isEmpty());
    }

    @Test
    void analyzesOneColumnAndIgnoresUnannotatedFields() {
        var columns = analysis.analize(Single.class);
        assertEquals(1, columns.size());
        assertEquals("id", columns.get(0).getName());
        assertEquals(int.class, columns.get(0).getType());
    }

    @Test
    void readsPrivateProtectedAndNullValues() throws Exception {
        var columns = analysis.analize(Multiple.class);
        assertEquals(3, columns.size());
        var rows = values.analyze(new Multiple());
        assertEquals(3, rows.size());
        assertEquals(7, value(rows, "id"));
        assertEquals("Carvel", value(rows, "name"));
        assertNull(value(rows, "optional"));
    }

    @Test
    void rejectsEmptyColumnNameInBothAnalyzers() {
        assertInvalid(new Blank());
    }

    @Test
    void rejectsWhitespaceColumnNameInBothAnalyzers() {
        assertInvalid(new Whitespace());
    }

    @Test
    void rejectsDuplicateColumnNamesIgnoringCaseInBothAnalyzers() {
        assertInvalid(new Duplicate());
    }

    private void assertInvalid(Object entity) {
        assertThrows(IllegalArgumentException.class, () -> analysis.analize(entity.getClass()));
        assertThrows(IllegalArgumentException.class, () -> values.analyze(entity));
    }

    private Object value(java.util.List<Row> rows, String name) {
        return rows.stream().filter(row -> name.equals(row.getColumnName())).findFirst().orElseThrow().getValue();
    }

    static class Empty { String ignored; }
    static class Single {
        @Column(columName = "id") private int id;
        String ignored;
    }
    static class Multiple {
        @Column(columName = "id") private int id = 7;
        @Column(columName = "name") protected String name = "Carvel";
        @Column(columName = "optional") private Integer optional;
        String ignored = "ignore me";
    }
    static class Blank { @Column(columName = "") String name; }
    static class Whitespace { @Column(columName = "  ") String name; }
    static class Duplicate {
        @Column(columName = "name") String first;
        @Column(columName = "NAME") String second;
    }
}
