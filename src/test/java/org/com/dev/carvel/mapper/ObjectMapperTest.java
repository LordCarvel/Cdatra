package org.com.dev.carvel.mapper;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rejectsNullRows() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(null, User.class));
    }

    @Test
    void rejectsEmptyRows() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(List.of(), User.class));
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(List.of(new Row("id", 1)), null));
    }

    @Test
    void rejectsNullRowInsideList() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(Arrays.asList((Row) null), User.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidColumnName(String name) {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(List.of(new Row(name, 1)), User.class));
    }

    @Test
    void mapsIgnoringCaseAndExtraColumns() throws Exception {
        var user = (User) mapper.map(List.of(new Row("ID", 7), new Row("NAME", "Carvel"),
                new Row("USER_EMAIL", "a@b.com"), new Row("extra", "ignored")), User.class);
        assertEquals(7, user.getId());
        assertEquals("Carvel", user.getName());
        assertEquals("a@b.com", user.getEmail());
        assertNull(user.getTemporary());
    }

    @Test
    void rejectsMissingAnnotatedColumn() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> mapper.map(List.of(new Row("id", 7), new Row("name", "Carvel")), User.class));
        assertTrue(error.getMessage().contains("user_email"));
    }

    @Test
    void allowsNullObjectFields() throws Exception {
        var entity = (Nullable) mapper.map(Arrays.asList(new Row("name", null), new Row("count", null)), Nullable.class);
        assertNull(entity.name);
        assertNull(entity.count);
    }

    @Test
    void rejectsNullPrimitiveField() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(Arrays.asList(new Row("id", null)), User.class));
    }

    @Test
    void rejectsIncompatibleFieldType() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(List.of(new Row("id", "seven")), User.class));
    }

    @Test
    void explainsMissingNoArgumentConstructor() {
        var error = assertThrows(NoSuchMethodException.class,
                () -> mapper.map(List.of(new Row("id", 1)), NoDefaultConstructor.class));
        assertTrue(error.getMessage().contains("no-argument constructor"));
        assertTrue(error.getMessage().contains(NoDefaultConstructor.class.getName()));
    }

    public static class Nullable {
        @Column(columName = "name") private String name;
        @Column(columName = "count") private Integer count;
        public Nullable() {}
    }

    public static class NoDefaultConstructor {
        @Column(columName = "id") private int id;
        public NoDefaultConstructor(int id) { this.id = id; }
    }
}
