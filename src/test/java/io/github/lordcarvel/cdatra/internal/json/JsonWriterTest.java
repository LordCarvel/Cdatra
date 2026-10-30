package io.github.lordcarvel.cdatra.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonWriterTest {

    private final JsonWriter writer = new JsonWriter();

    @Test
    void writesNullBooleansAndNumbers () {

        assertEquals("null", writer.write(null));
        assertEquals("true", writer.write(true));
        assertEquals("false", writer.write(false));
        assertEquals("42", writer.write(42));
        assertEquals("9007199254740991", writer.write(9007199254740991L));
        assertEquals("-1.5", writer.write(-1.5));
    }

    @Test
    void writesPlainStringsBetweenQuotes () {

        assertEquals("\"\"", writer.write(""));
        assertEquals("\"Carvel\"", writer.write("Carvel"));
        assertEquals("\"olá, ação\"", writer.write("olá, ação"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "a\"b|\"a\\\"b\"",
            "a\\b|\"a\\\\b\"",
            "a\"b\\c|\"a\\\"b\\\\c\""
    })
    void escapesQuotesAndBackslashes (String text, String expected) {

        assertEquals(expected, writer.write(text));
    }

    @Test
    void escapesControlCharactersAsUnicode () {

        assertEquals("\"a\\u000ab\"", writer.write("a\nb"));
        assertEquals("\"\\u0009\\u000d\\u0000\\u001f\"", writer.write("\t\r\u0000\u001f"));
        assertEquals("\" ~\"", writer.write(" ~"));
    }

    @Test
    void writesListsWithCommasBetweenItems () {

        assertEquals("[]", writer.write(List.of()));
        assertEquals("[1]", writer.write(List.of(1)));
        assertEquals("[1,\"a\",true]", writer.write(List.of(1, "a", true)));
        assertEquals("[null,2]", writer.write(Arrays.asList(null, 2)));
    }

    @Test
    void writesNestedLists () {

        assertEquals("[1,[2,3],[]]", writer.write(List.of(1, List.of(2, 3), List.of())));
        assertEquals("[[[\"deep\"]]]", writer.write(List.of(List.of(List.of("deep")))));
    }

    @Test
    void writesMapsInTheirIterationOrder () {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", 1);
        map.put("a", List.of(true));
        map.put("c", null);

        assertEquals("{}", writer.write(Map.of()));
        assertEquals("{\"b\":1,\"a\":[true],\"c\":null}", writer.write(map));
    }

    @Test
    void escapesMapKeys () {

        assertEquals("{\"a\\\"b\":1}", writer.write(Map.of("a\"b", 1)));
    }

    @Test
    void writesTheWorkerQueryRequest () {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sql", "SELECT * FROM records WHERE name = ? AND active = ?");
        request.put("params", List.of("O'Brien \"Bob\"", 1));

        assertEquals("{\"sql\":\"SELECT * FROM records WHERE name = ? AND active = ?\",\"params\":[\"O'Brien \\\"Bob\\\"\",1]}", writer.write(request));
    }

    @Test
    void writesMapsInsideListsAndListsInsideMaps () {

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("rows", List.of(List.of(1, "x")));

        assertEquals("[{\"rows\":[[1,\"x\"]]}]", writer.write(List.of(inner)));
    }

    @Test
    void rejectsNonStringKeys () {

        Map<Object, Object> numberKey = new HashMap<>();
        numberKey.put(1, "a");

        Map<Object, Object> nullKey = new HashMap<>();
        nullKey.put(null, "a");

        assertThrows(IllegalArgumentException.class, () -> writer.write(numberKey));
        assertThrows(IllegalArgumentException.class, () -> writer.write(nullKey));
    }

    @Test
    void rejectsUnsupportedValuesAtAnyDepth () {

        List<Object> nested = new ArrayList<>();
        nested.add(1);
        nested.add(new Object());

        assertThrows(IllegalArgumentException.class, () -> writer.write(new Object()));
        assertThrows(IllegalArgumentException.class, () -> writer.write('c'));
        assertThrows(IllegalArgumentException.class, () -> writer.write(nested));
        assertThrows(IllegalArgumentException.class, () -> writer.write(Map.of("a", new Object())));
    }
}
