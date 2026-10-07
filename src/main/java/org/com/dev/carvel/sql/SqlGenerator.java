package org.com.dev.carvel.sql;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.table.Table;

import java.util.ArrayList;
import java.util.List;

public class SqlGenerator {

    private final TypeMapper typeMapper = new TypeMapper();

    public String createTable(Table table) {
        validateTable(table);
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(table.getName());
        sql.append(" (");
        List<ColumnDefinition> columns = table.getColumnDefinitions();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            sql.append(column.getName()).append(" ").append(typeMapper.map(column.getType()));
            if (i < columns.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(");");
        return sql.toString();
    }

    private void validateTable(Table table) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        String tableName = table.getName();

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank"
            );
        }

        List<ColumnDefinition> columns = table.getColumnDefinitions();

        if (columns == null) {
            throw new IllegalArgumentException(
                    "Table columns cannot be null"
            );
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table requires at least one column"
            );
        }

        List<String> seenColumns = new ArrayList<>();

        for (ColumnDefinition columnDefinition : columns) {

            if (columnDefinition == null) {
                throw new IllegalArgumentException(
                        "Table column cannot be null"
                );
            }

            String columnName = columnDefinition.getName();

            if (columnName == null || columnName.isBlank()) {
                throw new IllegalArgumentException(
                        "Column name cannot be null or blank"
                );
            }

            boolean duplicated = false;

            for (String seenColumn : seenColumns) {

                if (columnName.equalsIgnoreCase(seenColumn)) {
                    duplicated = true;
                }
            }

            if (duplicated) {
                throw new IllegalArgumentException(
                        "Duplicate column in table: " + columnName
                );
            }

            seenColumns.add(columnName);

            Class<?> javaType = columnDefinition.getType();

            if (javaType == null) {
                throw new IllegalArgumentException(
                        "Column type cannot be null"
                );
            }

            typeMapper.map(javaType);
        }
    }

    public String insert(Table table, List<Row> rows) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Insert table cannot be null"
            );
        }

        if (rows == null) {
            throw new IllegalArgumentException(
                    "Insert rows cannot be null"
            );
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Insert requires at least one row"
            );
        }

        for (Row row : rows) {

            if (row == null) {
                throw new IllegalArgumentException(
                        "Insert row cannot be null"
                );
            }

            if (row.getColumnName() == null || row.getColumnName().isBlank()) {
                throw new IllegalArgumentException(
                        "Column name cannot be null or blank"
                );
            }
        }

        validateTable(table);

        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO ");
        sql.append(table.getName());
        sql.append(" (");

        List<String> seenColumns = new ArrayList<>();

        for (Row row : rows) {

            boolean duplicated = false;

            for (String seenColumn : seenColumns) {

                if (row.getColumnName().equalsIgnoreCase(seenColumn)) {
                    duplicated = true;
                }
            }

            if (duplicated) {
                throw new IllegalArgumentException(
                        "Duplicate column in insert: " + row.getColumnName()
                );
            }

            seenColumns.add(row.getColumnName());
        }

        for (Row row : rows) {

            boolean found = false;

            for (ColumnDefinition columnDefinition : table.getColumnDefinitions()) {

                if (row.getColumnName().equalsIgnoreCase(columnDefinition.getName())) {

                    found = true;

                    if (row.getValue() != null) {

                        SqlType expectedType =
                                typeMapper.map(columnDefinition.getType());

                        SqlType receivedType =
                                typeMapper.map(row.getValue().getClass());

                        if (expectedType != receivedType) {

                            throw new IllegalArgumentException(
                                    "Invalid value type for column: " + row.getColumnName()
                            );
                        }
                    }
                }
            }

            if (!found) {
                throw new IllegalArgumentException(
                        "Column not found in table: " + row.getColumnName()
                );
            }
        }

        for (int i = 0; i < rows.size(); ) {

            sql.append(rows.get(i).getColumnName());

            if (i < rows.size() - 1) {
                sql.append(", ");
            }

            i = i + 1;
        }

        sql.append(")");

        StringBuilder sqlValue = new StringBuilder();

        sqlValue.append(" VALUES (");

        for (int i = 0; i < rows.size(); ) {

            Object value = rows.get(i).getValue();

            if (value == null) {

                sqlValue.append("NULL");

            } else {

                SqlType sqlType = typeMapper.map(value.getClass());

                if (sqlType == SqlType.VARCHAR) {

                    String stringValue = value.toString();

                    stringValue = stringValue.replace("'", "''");

                    sqlValue.append("'");
                    sqlValue.append(stringValue);
                    sqlValue.append("'");

                } else {

                    sqlValue.append(value);
                }
            }

            if (i < rows.size() - 1) {
                sqlValue.append(", ");
            }

            i = i + 1;
        }

        sql.append(sqlValue);

        sql.append(");");

        return sql.toString();
    }
}
