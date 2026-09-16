package org.com.dev.carvel.sql;

public class TypeMapper {
    public SqlType map (Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return SqlType.INTEGER;

        } else if (type == String.class) {
            return SqlType.VARCHAR;

        } else if (type == long.class || type == Long.class) {
            return SqlType.BIGINT;

        } else if (type == boolean.class || type == Boolean.class) {
            return SqlType.BOOLEAN;

        } else if (type == double.class || type == Double.class) {
            return SqlType.DOUBLE;
        }

        throw new IllegalArgumentException("Unsupported Java type: " + type.getName());
    }
}
