package io.github.lordcarvel.cdatra.mapping;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.support.TestEntities;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperEdgeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsColumnsInArbitraryOrderAndDoesNotMutateRows () throws Exception {

        var rows = List.of(new Row("LABEL", "D'Ávila"), new Row("extra", 99), new Row("ID", 7));
        var entity = (TestEntities.ManualRecord) mapper.map(rows, TestEntities.ManualRecord.class);
        assertEquals(7, entity.id);
        assertEquals("D'Ávila", entity.label);
        assertEquals(3, rows.size());
        assertEquals("LABEL", rows.get(0).getColumnName());
        assertEquals("D'Ávila", rows.get(0).getValue());
    }

    @Test
    void mapsAllBoxedSupportedTypesAndNulls () throws Exception {

        var rows = List.of(new Row("integer_value", 7), new Row("long_value", Long.MAX_VALUE),
                new Row("boolean_value", true), new Row("double_value", 1.25));
        var entity = (Boxed) mapper.map(rows, Boxed.class);
        assertEquals(7, entity.integerValue);
        assertEquals(Long.MAX_VALUE, entity.longValue);
        assertTrue(entity.booleanValue);
        assertEquals(1.25, entity.doubleValue);
        var nulls = Arrays.asList(new Row("integer_value", null), new Row("long_value", null),
                new Row("boolean_value", null), new Row("double_value", null));
        entity = (Boxed) mapper.map(nulls, Boxed.class);
        assertNull(entity.integerValue);
        assertNull(entity.longValue);
        assertNull(entity.booleanValue);
        assertNull(entity.doubleValue);
    }

    @Test
    void mapsBoxedFieldsFromTheNumbersThatD1Returns () throws Exception {

        var rows = List.of(new Row("integer_value", 7L), new Row("long_value", 8L),
                new Row("boolean_value", 1L), new Row("double_value", 2L));
        var entity = (Boxed) mapper.map(rows, Boxed.class);
        assertEquals(7, entity.integerValue);
        assertEquals(8L, entity.longValue);
        assertTrue(entity.booleanValue);
        assertEquals(2.0, entity.doubleValue);
    }

    @Test
    void mapsPrimitiveFieldsFromTheNumbersThatD1Returns () throws Exception {

        var rows = List.of(new Row("int_value", 7L), new Row("boolean_value", 0L), new Row("double_value", 2L));
        var entity = (Primitives) mapper.map(rows, Primitives.class);
        assertEquals(7, entity.intValue);
        assertFalse(entity.booleanValue);
        assertEquals(2.0, entity.doubleValue);
    }

    @Test
    void rejectsNumbersThatDoNotFitTheField () {

        var outOfRange = List.of(new Row("int_value", 2147483648L), new Row("boolean_value", 0L), new Row("double_value", 1.5));
        assertThrows(IllegalArgumentException.class, () -> mapper.map(outOfRange, Primitives.class));

        var notABoolean = List.of(new Row("int_value", 1L), new Row("boolean_value", 2L), new Row("double_value", 1.5));
        assertThrows(IllegalArgumentException.class, () -> mapper.map(notABoolean, Primitives.class));
    }

    @Test
    void propagatesAnExceptionThrownByTheConstructor () {

        var error = assertThrows(InvocationTargetException.class, () -> mapper.map(List.of(new Row("label", "value")), FailingConstructor.class));
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("constructor failed", error.getCause().getMessage());
    }

    @Test
    void requiresAnAccessibleNoArgumentConstructor () {

        assertThrows(IllegalAccessException.class, () -> mapper.map(List.of(new Row("label", "value")), PrivateConstructor.class));
    }

    @Test
    void rejectsAbstractEntityInstantiation () {

        assertThrows(InstantiationException.class, () -> mapper.map(List.of(new Row("label", "value")), AbstractEntity.class));
    }

    @Test
    void retainsOriginalEntityDefaultsForUnannotatedFields () throws Exception {

        var entity = (Defaults) mapper.map(List.of(new Row("label", "database")), Defaults.class);
        assertEquals("database", entity.label);
        assertEquals("default", entity.ignored);
    }

    public static class Boxed {

        @Column(columName = "integer_value") public Integer integerValue;
        @Column(columName = "long_value") public Long longValue;
        @Column(columName = "boolean_value") public Boolean booleanValue;
        @Column(columName = "double_value") public Double doubleValue;

        public Boxed () {

        }
    }

    public static class Primitives {

        @Column(columName = "int_value") public int intValue;
        @Column(columName = "boolean_value") public boolean booleanValue;
        @Column(columName = "double_value") public double doubleValue;

        public Primitives () {

        }
    }

    public static class FailingConstructor {

        @Column(columName = "label") public String label;

        public FailingConstructor () {

            throw new IllegalStateException("constructor failed");
        }
    }

    public static class PrivateConstructor {

        @Column(columName = "label") public String label;

        private PrivateConstructor () {

        }
    }

    public abstract static class AbstractEntity {

        @Column(columName = "label") public String label;

        public AbstractEntity () {

        }
    }

    public static class Defaults {

        @Column(columName = "label") public String label;
        public String ignored = "default";

        public Defaults () {

        }
    }
}
