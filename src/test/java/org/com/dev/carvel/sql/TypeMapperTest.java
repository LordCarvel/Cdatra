package org.com.dev.carvel.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TypeMapperTest {

    private final TypeMapper mapper = new TypeMapper();

    static Stream<org.junit.jupiter.params.provider.Arguments> supportedTypes () {

        return Stream.of(arguments(int.class, SqlType.INTEGER), arguments(Integer.class, SqlType.INTEGER),
                arguments(String.class, SqlType.VARCHAR), arguments(long.class, SqlType.BIGINT),
                arguments(Long.class, SqlType.BIGINT), arguments(boolean.class, SqlType.BOOLEAN),
                arguments(Boolean.class, SqlType.BOOLEAN), arguments(double.class, SqlType.DOUBLE),
                arguments(Double.class, SqlType.DOUBLE));
    }

    @ParameterizedTest
    @MethodSource("supportedTypes")
    void mapsPrimitiveAndBoxedTypes (Class<?> type, SqlType expected) {

        assertEquals(expected, mapper.map(type));
    }

    static Stream<Class<?>> unsupportedTypes () {

        return Stream.of(Object.class, float.class, Float.class, short.class, byte.class, char.class,
                BigDecimal.class, LocalDate.class, String[].class, void.class);
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void rejectsUnsupportedTypes (Class<?> type) {

        var error = assertThrows(IllegalArgumentException.class, () -> mapper.map(type));
        assertEquals("Unsupported Java type: " + type.getName(), error.getMessage());
    }

    @Test
    void rejectsNullType () {

        assertEquals("Java type cannot be null", assertThrows(IllegalArgumentException.class, () -> mapper.map(null)).getMessage());
    }
}
