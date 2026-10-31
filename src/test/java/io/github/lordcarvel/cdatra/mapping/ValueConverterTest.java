package io.github.lordcarvel.cdatra.mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ValueConverterTest {

    private final ValueConverter converter = new ValueConverter();

    @Test
    void rejectsNullFieldType () {

        assertThrows(IllegalArgumentException.class, () -> converter.convert(1L, null));
    }

    @Test
    void convertsLongToInt () {

        assertEquals(30, converter.convert(30L, int.class));
        assertEquals(30, converter.convert(30L, Integer.class));
    }

    @Test
    void acceptsTheLimitsOfInt () {

        assertEquals(Integer.MAX_VALUE, converter.convert((long) Integer.MAX_VALUE, int.class));
        assertEquals(Integer.MIN_VALUE, converter.convert((long) Integer.MIN_VALUE, int.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {2147483648L, -2147483649L, Long.MAX_VALUE})
    void rejectsLongsOutsideTheLimitsOfInt (long value) {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> converter.convert(value, int.class));

        assertEquals("Value out of range for int: " + value, error.getMessage());
    }

    @Test
    void convertsZeroAndOneToBoolean () {

        assertEquals(false, converter.convert(0L, boolean.class));
        assertEquals(true, converter.convert(1L, boolean.class));
        assertEquals(false, converter.convert(0L, Boolean.class));
        assertEquals(true, converter.convert(1L, Boolean.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, -1L})
    void rejectsOtherNumbersAsBoolean (long value) {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> converter.convert(value, boolean.class));

        assertEquals("Value is not a boolean: " + value, error.getMessage());
    }

    @Test
    void convertsLongToDouble () {

        assertEquals(2.0, converter.convert(2L, double.class));
        assertEquals(2.0, converter.convert(2L, Double.class));
    }

    @Test
    void keepsLongForLongFields () {

        assertEquals(5L, converter.convert(5L, long.class));
        assertEquals(5L, converter.convert(5L, Long.class));
    }

    @Test
    void keepsValuesThatAlreadyHaveTheFieldType () {

        assertEquals("Ana", converter.convert("Ana", String.class));
        assertEquals(1.5, converter.convert(1.5, double.class));
        assertEquals(true, converter.convert(true, boolean.class));
        assertEquals(30, converter.convert(30, int.class));
    }

    @Test
    void keepsNull () {

        assertNull(converter.convert(null, String.class));
        assertNull(converter.convert(null, Integer.class));
    }
}
