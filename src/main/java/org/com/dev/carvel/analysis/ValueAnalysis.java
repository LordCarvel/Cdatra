package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.row.Row;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ValueAnalysis {
    public List<Row> analyze(Object value) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = value.getClass().getDeclaredFields();

        List<Row> rows = new ArrayList<>();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);

                field.setAccessible(true);

                Object fieldValue = field.get(value);

                rows.add(new Row(column.columName(), fieldValue));
            }
        }

        return rows;
    }
}
