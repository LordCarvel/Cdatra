package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Analysis {

    public List<ColumnDefinition> analize (Class<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("Analyzed type cannot be null");
        }
        Field[] fields = value.getDeclaredFields();

        List<ColumnDefinition> columns = new ArrayList<>();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String name = column.columName();
                if (name.isBlank()) {
                    throw new IllegalArgumentException("Column name cannot be blank");
                }
                for (ColumnDefinition existing : columns) {
                    if (name.equalsIgnoreCase(existing.getName())) {
                        throw new IllegalArgumentException("Duplicate column in entity: " + name);
                    }
                }

                columns.add(new ColumnDefinition(column.columName(), field.getType()));
            }
        }

        return columns;
    }
}
