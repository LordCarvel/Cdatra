package io.github.lordcarvel.cdatra.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class NamingTest {

    @ParameterizedTest
    @CsvSource({
            "User, user",
            "UserAccount, user_account",
            "createdAt, created_at",
            "id, id",
            "address2, address2",
            "user_name, user_name",
            "A, a"
    })
    void convertsJavaNamesToSnakeCase (String name, String expected) {

        assertEquals(expected, Naming.toSnakeCase(name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsNullOrEmptyNames (String name) {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Naming.toSnakeCase(name));

        assertEquals("Name cannot be null or empty", exception.getMessage());
    }

    @Test
    void producesNamesAcceptedByTheIdentifierValidation () {

        assertTrue(Naming.toSnakeCase("UserAccount").matches("[A-Za-z_][A-Za-z0-9_]*"));
    }
}
