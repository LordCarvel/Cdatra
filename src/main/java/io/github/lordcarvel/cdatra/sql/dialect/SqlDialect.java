package io.github.lordcarvel.cdatra.sql.dialect;

import io.github.lordcarvel.cdatra.sql.SqlType;

public interface SqlDialect {

    String typeName (SqlType type);

    String generatedIdColumn (String columnName, SqlType type);
}
