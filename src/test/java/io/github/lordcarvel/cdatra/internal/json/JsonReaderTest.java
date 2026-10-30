package io.github.lordcarvel.cdatra.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JsonReaderTest {

    @Test
    void rejectsNullText () {

        assertThrows(IllegalArgumentException.class, () -> new JsonReader(null));
    }

    @Test
    void readsLiterals () {

        assertNull(new JsonReader("null").read());
        assertEquals(true, new JsonReader("true").read());
        assertEquals(false, new JsonReader("false").read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nullx", "truefalse", "false0", "nullnull"})
    void rejectsContentAfterTheValue (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "x", "xnull", "nul", "tru", "fals", "NULL", "True"})
    void rejectsTextThatIsNotAValueAtTheCurrentPosition (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @Test
    void errorMessageNamesThePosition () {

        JsonReader reader = new JsonReader("nullx");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertTrue(error.getMessage().contains("position 4"));
    }
}
