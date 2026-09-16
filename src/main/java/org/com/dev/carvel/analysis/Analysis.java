package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Analysis {

    public List<ColumnDefinition> analize (Class value) {
        Field[] fields = value.getDeclaredFields();

        List<ColumnDefinition> columns = new ArrayList<>();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);

                columns.add(new ColumnDefinition(column.columName(), field.getType()));

                System.out.println("column name: " + column.columName() + "|| column type: "+ field.getType());
            }
        }

        return columns;
    }
}
