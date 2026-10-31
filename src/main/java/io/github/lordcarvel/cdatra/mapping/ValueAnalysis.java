package io.github.lordcarvel.cdatra.mapping;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;
import io.github.lordcarvel.cdatra.exception.CdatraException;
import io.github.lordcarvel.cdatra.metadata.Analysis;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValueAnalysis {

    private final ValueConverter valueConverter = new ValueConverter();

    public List<Row> analyze (Object value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed value cannot be null");
        }

        new Analysis().analize(value.getClass());

        List<Field> fields = new ArrayList<>();
        List<Row> rows = new ArrayList<>();

        for (Class<?> current = value.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {

            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Field field : fields) {

            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {

                continue;
            }

            if (field.isAnnotationPresent(Column.class)) {

                if (field.isAnnotationPresent(GeneratedValue.class)) {

                    continue;
                }

                Column column = field.getAnnotation(Column.class);

                rows.add(new Row(column.columName(), readField(field, value)));
            }
        }

        return rows;
    }

    public Row analyzeId (Object value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed value cannot be null");
        }

        new Analysis().analize(value.getClass());
        List<Field> fields = new ArrayList<>();

        for (Class<?> current = value.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {

            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Field field : fields) {

            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {

                continue;
            }

            if (field.isAnnotationPresent(Id.class)) {

                if (!field.isAnnotationPresent(Column.class)) {

                    throw new IllegalArgumentException("@Id field must also be annotated with @Column");
                }

                Column column = field.getAnnotation(Column.class);

                return new Row(column.columName(), readField(field, value));
            }
        }

        throw new IllegalArgumentException("Entity does not contain an @Id field");
    }

    public void setGeneratedValue (Object value, Object generatedValue) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed value cannot be null");
        }

        new Analysis().analize(value.getClass());
        List<Field> fields = new ArrayList<>();

        for (Class<?> current = value.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {

            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Field field : fields) {

            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {

                continue;
            }

            if (field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(GeneratedValue.class)) {

                field.setAccessible(true);

                try {

                    field.set(value, valueConverter.convert(generatedValue, field.getType()));
                } catch (IllegalAccessException exception) {

                    throw new CdatraException("Failed to set field " + field.getName(), exception);
                }

                return;
            }
        }
    }

    private Object readField (Field field, Object value) {

        field.setAccessible(true);

        try {

            return field.get(value);
        } catch (IllegalAccessException exception) {

            throw new CdatraException("Failed to read field " + field.getName(), exception);
        }
    }
}
