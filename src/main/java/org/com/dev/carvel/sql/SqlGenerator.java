package org.com.dev.carvel.sql;


import org.com.dev.carvel.columnDefinition.ColumnDefinition;
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
}
