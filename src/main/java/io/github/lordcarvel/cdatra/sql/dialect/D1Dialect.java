package io.github.lordcarvel.cdatra.sql.dialect;

import io.github.lordcarvel.cdatra.sql.SqlType;

public class D1Dialect implements SqlDialect {

    @Override
    public String typeName (SqlType type) {

        if (type == null) {

            throw new IllegalArgumentException("SQL type cannot be null");
        }

        if (type == SqlType.INTEGER || type == SqlType.BIGINT || type == SqlType.BOOLEAN) {

            return "INTEGER";
        } else if (type == SqlType.VARCHAR) {

            return "TEXT";
        } else if (type == SqlType.DOUBLE) {

            return "REAL";
        }

        throw new IllegalArgumentException("Unsupported SQL type: " + type.name());
    }

    @Override
    public String generatedIdColumn (String columnName, SqlType type) {

        return columnName + " " + typeName(type) + " PRIMARY KEY AUTOINCREMENT";
    }
}
