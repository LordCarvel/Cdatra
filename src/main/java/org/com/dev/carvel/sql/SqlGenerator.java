package org.com.dev.carvel.sql;


import org.com.dev.carvel.columnDefinition.ColumnDefinition;
import org.com.dev.carvel.row.Row;
import org.com.dev.carvel.table.Table;

import java.util.List;

public class SqlGenerator {
    private final TypeMapper typeMapper = new TypeMapper();

    public String createTable (Table table) {
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

            if (i < columns.size() -  1) {
                sql.append(", ");
            }

            i = i + 1;
        }

        sql.append(");");

        return sql.toString();
    }

    public String insert (Table table, List<Row> rows) {

        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO ");
        sql.append(table.getName());
        sql.append(" (");

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
                throw new IllegalArgumentException("Column value cannot be null");
            }

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
