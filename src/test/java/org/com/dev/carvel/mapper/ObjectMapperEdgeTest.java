package org.com.dev.carvel.mapper;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.support.TestEntities;
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
