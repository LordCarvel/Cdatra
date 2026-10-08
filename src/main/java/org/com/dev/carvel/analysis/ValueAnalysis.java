package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.annotations.GeneratedValue;
import org.com.dev.carvel.annotations.Id;
import org.com.dev.carvel.row.Row;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ValueAnalysis {

    public List<Row> analyze(Object value) throws IllegalAccessException {

        if (value == null) {

            throw new IllegalArgumentException(
                    "Analyzed value cannot be null"
            );
        }

        new Analysis().analize(value.getClass());

        Field[] fields = value.getClass().getDeclaredFields();

        List<Row> rows = new ArrayList<>();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Column.class)) {

                if (field.isAnnotationPresent(GeneratedValue.class)) {

                    continue;
                }

                Column column = field.getAnnotation(Column.class);

                field.setAccessible(true);

                Object fieldValue = field.get(value);

                rows.add(new Row(column.columName(), fieldValue));
            }
        }

        return rows;
    }

    public Row analyzeId (Object value) throws IllegalAccessException {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed value cannot be null");
        }

        Field[] fields = value.getClass().getDeclaredFields();

        for (Field field: fields) {

            if (field.isAnnotationPresent(Id.class)) {

                if (!field.isAnnotationPresent(Column.class)) {

                    throw new IllegalArgumentException(
                            "@Id field must also be annotated with @Column"
                    );
                }

                Column column = field.getAnnotation(Column.class);

                field.setAccessible(true);

                Object fieldValue = field.get(value);

                return new Row(
                        column.columName(),
                        fieldValue
                );
            }
        }

        throw new IllegalArgumentException(

                "Entity does not contain an @Id field"
        );
    }

    public void setGeneratedValue (Object value, Object generatedValue) throws IllegalAccessException {

        if (value == null) {

            throw new IllegalArgumentException(
                    "Analyzed value cannot be null"
            );
        }

        Field[] fields = value.getClass().getDeclaredFields();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(GeneratedValue.class)) {

                field.setAccessible(true);

                field.set(value, generatedValue);

                return;
            }
        }
    }
}
