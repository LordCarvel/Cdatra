package io.github.lordcarvel.cdatra.mapping;

public class ValueConverter {

    public Object convert (Object value, Class<?> fieldType) {

        if (fieldType == null) {

            throw new IllegalArgumentException("Field type cannot be null");
        }

        if (value instanceof Long) {

            return convertLong((Long) value, fieldType);
        }

        return value;
    }

    private Object convertLong (Long value, Class<?> fieldType) {

        if (fieldType == int.class || fieldType == Integer.class) {

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {

                throw new IllegalArgumentException("Value out of range for int: " + value);
            }

            return value.intValue();
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {

            if (value == 0) {

                return false;
            }

            if (value == 1) {

                return true;
            }

            throw new IllegalArgumentException("Value is not a boolean: " + value);
        }

        if (fieldType == double.class || fieldType == Double.class) {

            return value.doubleValue();
        }

        return value;
    }
}
