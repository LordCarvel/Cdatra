package io.github.lordcarvel.cdatra.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void skipsWhitespaceAroundTheValue () {

        assertEquals(true, new JsonReader(" true").read());
        assertEquals(true, new JsonReader("true ").read());
        assertEquals(false, new JsonReader(" \t\r\n false \r\n\t ").read());
        assertNull(new JsonReader("\n\n  null\n").read());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "   ", "\n", "\t\r\n"})
    void rejectsTextWithOnlyWhitespace (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\fnull", "null\f", " null", "null "})
    void rejectsWhitespaceThatJsonDoesNotAllow (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null true", "true\nfalse", "nu ll"})
    void whitespaceDoesNotJoinTwoValues (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @Test
    void readsIntegersAsLong () {

        assertEquals(0L, new JsonReader("0").read());
        assertEquals(7L, new JsonReader("7").read());
        assertEquals(42L, new JsonReader("42").read());
        assertEquals(-12L, new JsonReader("-12").read());
        assertEquals(9007199254740991L, new JsonReader("9007199254740991").read());
    }

    @Test
    void readsTheLimitsOfLong () {

        assertEquals(Long.MAX_VALUE, new JsonReader("9223372036854775807").read());
        assertEquals(Long.MIN_VALUE, new JsonReader("-9223372036854775808").read());
    }

    @Test
    void readsIntegersThatDoNotStartAtPositionZero () {

        assertEquals(12L, new JsonReader(" 12").read());
        assertEquals(-12L, new JsonReader("\n  -12  ").read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12x", "1 2", "-1-2", "12null"})
    void rejectsContentAfterTheNumber (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "--1", "-x", "+1", "9223372036854775808", "-9223372036854775809"})
    void rejectsTextThatIsNotAnInteger (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @Test
    void readsDecimalsAsDouble () {

        assertEquals(1.5, new JsonReader("1.5").read());
        assertEquals(-0.25, new JsonReader("-0.25").read());
        assertEquals(10.125, new JsonReader("10.125").read());
        assertEquals(0.0, new JsonReader("0.0").read());
    }

    @Test
    void readsWholeDecimalsAsDoubleAndNotAsLong () {

        assertInstanceOf(Double.class, new JsonReader("1.0").read());
        assertInstanceOf(Long.class, new JsonReader("1").read());
    }

    @Test
    void readsDecimalsThatDoNotStartAtPositionZero () {

        assertEquals(1.5, new JsonReader(" 1.5").read());
        assertEquals(-1.5, new JsonReader("\n  -1.5  ").read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2.3", "1.5x", "1.5 2", ".5"})
    void rejectsContentThatDoesNotFormADecimal (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @Test
    void readsExponentsAsDouble () {

        assertEquals(2000.0, new JsonReader("2e3").read());
        assertEquals(2000.0, new JsonReader("2E3").read());
        assertEquals(2000.0, new JsonReader("2e+3").read());
        assertEquals(0.005, new JsonReader("5e-3").read());
        assertEquals(150.0, new JsonReader("1.5e2").read());
        assertEquals(-100.0, new JsonReader("-1e2").read());
        assertEquals(0.0015, new JsonReader(" 1.5E-3 ").read());
    }

    @Test
    void exponentWithWholeResultIsStillDouble () {

        assertInstanceOf(Double.class, new JsonReader("2e3").read());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2e", "2E", "2e+", "2e-", "2ex", "2e3e4", "2e3.5", "e5"})
    void rejectsContentThatDoesNotFormAnExponent (String text) {

        JsonReader reader = new JsonReader(text);

        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "-|1",
            "-.5|1",
            "-e2|1",
            "1.|2",
            "1.e2|2",
            "-1.|3",
            "2e|2",
            "2E|2",
            "2e+|3",
            "2e-|3",
            "1.5e|4"
    })
    void requiresDigitsAndNamesWhereTheyWereMissing (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Expected a digit at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "00|0",
            "007|0",
            "01.5|0",
            "01e2|0",
            "-01|1",
            "-00|1",
            "' 007'|1"
    })
    void rejectsLeadingZeros (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Leading zeros are not allowed at position " + position, error.getMessage());
    }

    @Test
    void acceptsZeroWhenItIsNotALeadingZero () {

        assertEquals(0L, new JsonReader("-0").read());
        assertEquals(0.5, new JsonReader("0.5").read());
        assertEquals(0.0, new JsonReader("0e1").read());
        assertEquals(10L, new JsonReader("10").read());
        assertEquals(100L, new JsonReader("100").read());
        assertEquals(1.05, new JsonReader("1.05").read());
    }

    @Test
    void zerosAreAllowedAtTheStartOfTheExponent () {

        assertEquals(100000.0, new JsonReader("1e05").read());
    }

    @Test
    void errorMessageNamesThePosition () {

        JsonReader reader = new JsonReader("nullx");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertTrue(error.getMessage().contains("position 4"));
    }
}
