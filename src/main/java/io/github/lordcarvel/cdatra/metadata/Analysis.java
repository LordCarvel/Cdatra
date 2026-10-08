package io.github.lordcarvel.cdatra.metadata;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.Entity;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Analysis {

    public List<ColumnDefinition> analize (Class<?> value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed type cannot be null");
        }

        int idCount = 0;
        List<Field> fields = new ArrayList<>();
        List<ColumnDefinition> columns = new ArrayList<>();

        for (Class<?> current = value; current != null && current != Object.class; current = current.getSuperclass()) {

            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        for (Field field : fields) {

            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {

                continue;
            }

            if (field.isAnnotationPresent(GeneratedValue.class) && !field.isAnnotationPresent(Column.class)) {

                throw new IllegalArgumentException("@GeneratedValue field must also be annotated with @Column");
            }

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

                if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {

                    throw new IllegalArgumentException("Invalid column name: " + name);
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

                Class<?> type = field.getType();

                if (isGeneratedValue && type != int.class && type != Integer.class && type != long.class && type != Long.class) {

                    throw new IllegalArgumentException("Generated ID must use int, Integer, long or Long");
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
        String tableName = entity.tableNaame();

        if (tableName == null || !tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {

            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        return tableName;
    }

    public String analyzeIdColumnName (Class<?> value) {

        if (value == null) {

            throw new IllegalArgumentException("Analyzed type cannot be null");
        }

        for (ColumnDefinition column : analize(value)) {

            if (column.isId()) {

                return column.getName();
            }
        }

        throw new IllegalArgumentException("Entity does not contain an @Id field");
    }
}
