package io.github.lordcarvel.cdatra.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CdatraExceptionTest {

    @Test
    void keepsTheMessage () {

        CdatraException error = new CdatraException("Query failed");

        assertEquals("Query failed", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    void keepsTheMessageAndTheCause () {

        SQLException cause = new SQLException("Table not found");

        CdatraException error = new CdatraException("Query failed", cause);

        assertEquals("Query failed", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    void isUnchecked () {

        assertInstanceOf(RuntimeException.class, new CdatraException("Query failed"));
    }
}
