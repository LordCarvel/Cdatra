package org.com.dev.carvel.mapper;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.analysis.Analysis;
import org.com.dev.carvel.row.Row;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectMapper {

    public Object map (List<Row> rows, Class<?> type) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if (rows == null || rows.isEmpty()) {

            throw new IllegalArgumentException("Rows cannot be null or empty");
        }

        if (type == null) {

            throw new IllegalArgumentException("Mapped type cannot be null");
        }

        List<String> resultColumns = new ArrayList<>();

        for (Row row : rows) {

            if (row == null) {

                throw new IllegalArgumentException("Mapped row cannot be null");
            }

            if (row.getColumnName() == null || row.getColumnName().isBlank()) {

                throw new IllegalArgumentException("Mapped column name cannot be null or blank");
            }

            for (String columnName : resultColumns) {

                if (columnName.equalsIgnoreCase(row.getColumnName())) {

                    throw new IllegalArgumentException("Duplicate column in result: " + row.getColumnName());
                }
            }

            resultColumns.add(row.getColumnName());
        }

        new Analysis().analize(type);
        Constructor<?> constructor;

        try {

            constructor = type.getDeclaredConstructor();
        } catch (NoSuchMethodException exception) {

            throw new NoSuchMethodException("Mapped type requires a no-argument constructor: " + type.getName());
        }

        Object object = constructor.newInstance();
        List<Field> fields = new ArrayList<>();
        List<String> mappedColumns = new ArrayList<>();

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {

            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Row row : rows) {

            for (Field field : fields) {

                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && field.isAnnotationPresent(Column.class)) {

                    Column column = field.getAnnotation(Column.class);

                    if (row.getColumnName().equalsIgnoreCase(column.columName())) {

                        field.setAccessible(true);
                        field.set(object, row.getValue());

                        if (!mappedColumns.contains(column.columName())) {

                            mappedColumns.add(column.columName());
                        }
                    }
                }
            }
        }

        for (Field field : fields) {

            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && field.isAnnotationPresent(Column.class)) {

                Column column = field.getAnnotation(Column.class);

                if (!mappedColumns.contains(column.columName())) {

                    throw new IllegalArgumentException("Column not found in result: " + column.columName());
                }
            }
        }

        return object;
    }
}
