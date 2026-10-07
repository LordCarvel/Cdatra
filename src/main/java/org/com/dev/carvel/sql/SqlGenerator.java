package org.com.dev.carvel.sql;

import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.table.Table;

import java.util.ArrayList;
import java.util.List;

public class SqlGenerator {

    private final TypeMapper typeMapper = new TypeMapper();

    public String createTable(Table table) {

        String tableName = table.getName();

        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (");

        List<ColumnDefinition> columns = table.getColumnDefinitions();

        for (int i = 0; i < columns.size(); ) {

            ColumnDefinition columnDefinition = columns.get(i);

            String columnName = columnDefinition.getName();
            Class<?> javaType = columnDefinition.getType();

            SqlType sqlType = typeMapper.map(javaType);

            String columnSql = columnName + " " + sqlType;

            sql.append(columnSql);

            if (i < columns.size() - 1) {
                sql.append(", ");
            }

            i = i + 1;
        }

        sql.append(");");

        return sql.toString();
    }

    public String insert(Table table, List<Row> rows) {

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Insert requires at least one row"
            );
        }

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