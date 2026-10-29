package io.github.lordcarvel.cdatra.sql.dialect;

import io.github.lordcarvel.cdatra.sql.SqlType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class D1DialectTest {

    private final D1Dialect dialect = new D1Dialect();

    @ParameterizedTest
    @CsvSource({
            "INTEGER, INTEGER",
            "BIGINT, INTEGER",
            "BOOLEAN, INTEGER",
            "VARCHAR, TEXT",
            "DOUBLE, REAL"
    })
    void mapsSqlTypesToSqliteStorageTypes (SqlType type, String expected) {

        assertEquals(expected, dialect.typeName(type));
    }

    @ParameterizedTest
    @EnumSource(SqlType.class)
    void supportsEverySqlType (SqlType type) {

        assertNotNull(dialect.typeName(type));
    }

    @Test
    void writesGeneratedIdsAsIntegerAutoincrementEvenForBigint () {

        assertEquals("id INTEGER PRIMARY KEY AUTOINCREMENT", dialect.generatedIdColumn("id", SqlType.INTEGER));
        assertEquals("id INTEGER PRIMARY KEY AUTOINCREMENT", dialect.generatedIdColumn("id", SqlType.BIGINT));
    }

    @Test
    void usesTheGivenColumnName () {

        assertEquals("userId INTEGER PRIMARY KEY AUTOINCREMENT", dialect.generatedIdColumn("userId", SqlType.INTEGER));
    }

    @Test
    void rejectsNullType () {

        assertThrows(IllegalArgumentException.class, () -> dialect.typeName(null));
        assertThrows(IllegalArgumentException.class, () -> dialect.generatedIdColumn("id", null));
    }
}
