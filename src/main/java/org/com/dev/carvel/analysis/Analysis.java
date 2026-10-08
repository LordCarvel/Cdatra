package org.com.dev.carvel.analysis;

import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.annotations.Entity;
import org.com.dev.carvel.annotations.GeneratedValue;
import org.com.dev.carvel.annotations.Id;
import org.com.dev.carvel.columnDefinition.ColumnDefinition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Analysis {

    public List<ColumnDefinition> analize (Class<?> value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed type cannot be null");
        }

        int idCount = 0;
        Field[] fields = value.getDeclaredFields();
        List<ColumnDefinition> columns = new ArrayList<>();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Id.class)) {

                idCount = idCount + 1;

                if (!field.isAnnotationPresent(Column.class)) {

                    throw new IllegalArgumentException("@Id field must also be annotated with @Column");
                }
            }

            if (field.isAnnotationPresent(Column.class)) {

                Column column = field.getAnnotation(Column.class);
                String name = column.columName();

                if (name == null || name.isBlank()) {

                    throw new IllegalArgumentException("Column name cannot be null or blank");
                }

                for (ColumnDefinition existing : columns) {

                    if (name.equalsIgnoreCase(existing.getName())) {

                        throw new IllegalArgumentException("Duplicate column in entity: " + name);
                    }
                }

                boolean isId = field.isAnnotationPresent(Id.class);
                boolean isGeneratedValue = field.isAnnotationPresent(GeneratedValue.class);

                if (isGeneratedValue && !isId) {

                    throw new IllegalArgumentException("@GeneratedValue field must also be annotated with @Id");
                }

                columns.add(new ColumnDefinition(column.columName(), field.getType(), isId, isGeneratedValue));
            }
        }

        if (idCount > 1) {

            throw new IllegalArgumentException("Entity cannot contain more than one @Id field");
        }

        return columns;
    }

    public String analyzeTableName (Class<?> value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed class cannot be null");
        }

        if (!value.isAnnotationPresent(Entity.class)) {

            throw new IllegalArgumentException("Class is not annotated with @Entity");
        }

        Entity entity = value.getAnnotation(Entity.class);

        return entity.tableNaame();
    }

    public String analyzeIdColumnName (Class<?> value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed type cannot be null");
        }

        Field[] fields = value.getDeclaredFields();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Id.class)) {

                if (!field.isAnnotationPresent(Column.class)) {

                    throw new IllegalArgumentException("@Id field must also be annotated with @Column");
                }

                Column column = field.getAnnotation(Column.class);

                return column.columName();
            }
        }

        throw new IllegalArgumentException("Entity does not contain an @Id field");
    }
}
