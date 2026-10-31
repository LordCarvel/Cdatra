package io.github.lordcarvel.cdatra.d1;

import io.github.lordcarvel.cdatra.sql.dialect.D1Dialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class D1DatabaseAdapterTest {

    private static final String URL = "https://worker.example.com";

    private static final String SECRET = "test-secret";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidUrl (String url) {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new D1DatabaseAdapter(url, SECRET));

        assertEquals("Worker URL cannot be null or blank", error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidSecret (String secret) {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new D1DatabaseAdapter(URL, secret));

        assertEquals("Worker secret cannot be null or blank", error.getMessage());
    }

    @Test
    void usesD1Dialect () {

        assertInstanceOf(D1Dialect.class, new D1DatabaseAdapter(URL, SECRET).dialect());
    }
}
