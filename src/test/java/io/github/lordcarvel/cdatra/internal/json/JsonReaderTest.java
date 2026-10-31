package io.github.lordcarvel.cdatra.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "9223372036854775808|0",
            "-9223372036854775809|0",
            "99999999999999999999999999|0",
            "1e400|0",
            "-1e400|0",
            "1.5e999|0",
            "' 1e400'|1",
            "'  9223372036854775808'|2"
    })
    void rejectsNumbersOutOfRangeNamingWhereTheyStart (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Number out of range at position " + position, error.getMessage());
    }

    @Test
    void readsTheLargestFiniteDouble () {

        assertEquals(Double.MAX_VALUE, new JsonReader("1.7976931348623157e308").read());
    }

    @Test
    void readsStrings () {

        assertEquals("", new JsonReader("\"\"").read());
        assertEquals("ab", new JsonReader("\"ab\"").read());
        assertEquals("hello world", new JsonReader("\"hello world\"").read());
        assertEquals("ação ✓", new JsonReader("\"ação ✓\"").read());
    }

    @Test
    void keepsWhitespaceInsideTheString () {

        assertEquals("  ab  ", new JsonReader("\"  ab  \"").read());
    }

    @Test
    void readsStringsThatDoNotStartAtPositionZero () {

        assertEquals("ab", new JsonReader(" \"ab\"").read());
        assertEquals("ab", new JsonReader("\n  \"ab\"  ").read());
    }

    @Test
    void textBetweenQuotesIsAStringEvenWhenItLooksLikeAnotherValue () {

        assertEquals("null", new JsonReader("\"null\"").read());
        assertEquals("true", new JsonReader("\"true\"").read());
        assertEquals("12", new JsonReader("\"12\"").read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'\"'|0",
            "'\"ab'|0",
            "' \"ab'|1",
            "'  \"ab  '|2"
    })
    void rejectsUnterminatedStringsNamingWhereTheyStart (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated string at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'\"ab\"x'|4",
            "'\"ab\" \"cd\"'|5",
            "'\"\"\"\"'|2"
    })
    void rejectsContentAfterTheString (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected content at position " + position, error.getMessage());
    }

    @Test
    void readsSimpleEscapes () {

        assertEquals("a\"b", new JsonReader("\"a\\\"b\"").read());
        assertEquals("a\\b", new JsonReader("\"a\\\\b\"").read());
        assertEquals("a/b", new JsonReader("\"a\\/b\"").read());
        assertEquals("a\bb", new JsonReader("\"a\\bb\"").read());
        assertEquals("a\fb", new JsonReader("\"a\\fb\"").read());
        assertEquals("a\nb", new JsonReader("\"a\\nb\"").read());
        assertEquals("a\rb", new JsonReader("\"a\\rb\"").read());
        assertEquals("a\tb", new JsonReader("\"a\\tb\"").read());
    }

    @Test
    void readsConsecutiveEscapes () {

        assertEquals("\n\t", new JsonReader("\"\\n\\t\"").read());
        assertEquals("\\\\", new JsonReader("\"\\\\\\\\\"").read());
        assertEquals("\"\"", new JsonReader("\"\\\"\\\"\"").read());
    }

    @Test
    void escapedBackslashDoesNotHideTheClosingQuote () {

        assertEquals("a\\", new JsonReader("\"a\\\\\"").read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'\"ab\\'|0",
            "'\"a\\\"'|0",
            "' \"\\'|1"
    })
    void rejectsStringsThatEndInsideAnEscape (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated string at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'\"\\x\"'|2",
            "'\"a\\q\"'|3",
            "'\"\\N\"'|2"
    })
    void rejectsInvalidEscapesNamingWhereTheyAre (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid escape at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 8, 9, 10, 12, 13, 27, 31})
    void rejectsRawControlCharactersNamingWhereTheyAre (int code) {

        JsonReader reader = new JsonReader("\"a" + (char) code + "b\"");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid control character at position 2", error.getMessage());
    }

    @Test
    void controlCharacterPositionCountsFromTheStartOfTheText () {

        JsonReader reader = new JsonReader("  \"ab\n\"");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid control character at position 5", error.getMessage());
    }

    @Test
    void acceptsCharactersRightAfterTheControlRange () {

        assertEquals("a b", new JsonReader("\"a b\"").read());
        assertEquals("a\u007Fb", new JsonReader("\"a\u007Fb\"").read());
    }

    @Test
    void rawControlCharacterAfterABackslashIsAnInvalidEscape () {

        JsonReader reader = new JsonReader("\"a\\\nb\"");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid escape at position 3", error.getMessage());
    }

    @Test
    void readsUnicodeEscapes () {

        assertEquals("A", new JsonReader("\"\\u0041\"").read());
        assertEquals("café", new JsonReader("\"caf\\u00e9\"").read());
        assertEquals("café", new JsonReader("\"caf\\u00E9\"").read());
        assertEquals("\u0000", new JsonReader("\"\\u0000\"").read());
        assertEquals("\uFFFF", new JsonReader("\"\\uFFFF\"").read());
    }

    @Test
    void unicodeEscapeReadsExactlyFourDigits () {

        assertEquals("A1", new JsonReader("\"\\u00411\"").read());
        assertEquals("ABC", new JsonReader("\"\\u0041\\u0042\\u0043\"").read());
    }

    @Test
    void unicodeEscapeCanWriteAControlCharacter () {

        assertEquals("a\nb", new JsonReader("\"a\\u000Ab\"").read());
    }

    @Test
    void twoUnicodeEscapesFormAnEmoji () {

        assertEquals("😀", new JsonReader("\"\\uD83D\\uDE00\"").read());
    }

    @Test
    void readsControlCharactersWrittenByJsonWriter () {

        String original = "a\u0001b\u001Fc";

        String json = new JsonWriter().write(original);

        assertEquals(original, new JsonReader(json).read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'\"\\u12g4\"'|5",
            "'\"\\u12\"'|5",
            "'\"\\u12'|5",
            "'\"\\u'|3",
            "'\"\\u+123\"'|3"
    })
    void rejectsInvalidUnicodeEscapesNamingWhereTheyAre (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid unicode escape at position " + position, error.getMessage());
    }

    @Test
    void unicodeEscapeRejectsDigitsFromOtherAlphabets () {

        JsonReader reader = new JsonReader("\"\\u\u0660000\"");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid unicode escape at position 3", error.getMessage());
    }

    @Test
    void uppercaseUIsNotAUnicodeEscape () {

        JsonReader reader = new JsonReader("\"\\U0041\"");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Invalid escape at position 2", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[ ]", "[ \n\t ]"})
    void readsEmptyLists (String text) {

        assertEquals(List.of(), new JsonReader(text).read());
    }

    @ParameterizedTest
    @ValueSource(strings = {" []", "\n  [ ]  "})
    void readsListsThatDoNotStartAtPositionZero (String text) {

        assertEquals(List.of(), new JsonReader(text).read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'['|0",
            "'[  '|0",
            "' ['|1"
    })
    void rejectsUnterminatedListsNamingWhereTheyStart (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated list at position " + position, error.getMessage());
    }

    @Test
    void rejectsContentAfterTheList () {

        JsonReader reader = new JsonReader("[]x");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected content at position 2", error.getMessage());
    }

    @Test
    void closingBracketAloneIsNotAValue () {

        JsonReader reader = new JsonReader("]");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected JSON value at position 0", error.getMessage());
    }

    @Test
    void readsListsWithOneValueOfAnyType () {

        assertEquals(List.of(1L), new JsonReader("[1]").read());
        assertEquals(List.of(1.5), new JsonReader("[1.5]").read());
        assertEquals(List.of("ab"), new JsonReader("[\"ab\"]").read());
        assertEquals(List.of(true), new JsonReader("[true]").read());
        assertEquals(Arrays.asList((Object) null), new JsonReader("[null]").read());
    }

    @Test
    void readsListsInsideLists () {

        assertEquals(List.of(List.of()), new JsonReader("[[]]").read());
        assertEquals(List.of(List.of(7L)), new JsonReader("[[7]]").read());
        assertEquals(List.of(List.of(List.of("a"))), new JsonReader("[[[\"a\"]]]").read());
    }

    @Test
    void acceptsWhitespaceAroundTheValueInTheList () {

        assertEquals(List.of(1L), new JsonReader("[ 1 ]").read());
        assertEquals(List.of(List.of(1L)), new JsonReader("[ [ 1 ] ]").read());
    }

    @Test
    void closingBracketInsideAStringDoesNotCloseTheList () {

        assertEquals(List.of("]"), new JsonReader("[\"]\"]").read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'[1'|0",
            "'[1 '|0",
            "' [1'|1",
            "'[[]'|0",
            "'[[1]'|0",
            "'[['|1"
    })
    void rejectsUnterminatedListsWithValues (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated list at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'[x]'|1",
            "'[,]'|1",
            "'[ }]'|2"
    })
    void rejectsInvalidValuesInsideTheList (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected JSON value at position " + position, error.getMessage());
    }

    @Test
    void rejectsContentAfterTheListWithAValue () {

        JsonReader reader = new JsonReader("[1]x");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected content at position 3", error.getMessage());
    }

    @Test
    void readsListsWithSeveralValues () {

        assertEquals(List.of(1L, 2L, 3L), new JsonReader("[1,2,3]").read());
        assertEquals(Arrays.asList(1L, "Ana", true, null, 1.5), new JsonReader("[1,\"Ana\",true,null,1.5]").read());
    }

    @Test
    void acceptsWhitespaceAroundTheCommas () {

        assertEquals(List.of(1L, 2L), new JsonReader("[1, 2]").read());
        assertEquals(List.of(1L, 2L), new JsonReader("[ 1 , 2 ]").read());
        assertEquals(List.of(1L, 2L), new JsonReader("[\n  1,\n  2\n]").read());
    }

    @Test
    void readsRowsInTheWorkerFormat () {

        assertEquals(List.of(List.of(1L, "Ana"), List.of(2L, "Bia")), new JsonReader("[[1, \"Ana\"], [2, \"Bia\"]]").read());
    }

    @Test
    void commaInsideAStringDoesNotSeparateValues () {

        assertEquals(List.of("a,b"), new JsonReader("[\"a,b\"]").read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'[1 2]'|3",
            "'[1:2]'|2",
            "'[\"a\" \"b\"]'|5",
            "'[[1] 2]'|5"
    })
    void rejectsValuesWithoutACommaBetweenThem (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Expected ',' or ']' at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'[1,]'|3",
            "'[1, ]'|4",
            "'[,1]'|1",
            "'[1,,2]'|3"
    })
    void rejectsCommasWithoutAValue (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected JSON value at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'[1,'|0",
            "'[1, '|0",
            "'[1,2'|0",
            "'[[1,2],'|0"
    })
    void rejectsListsThatEndAfterAComma (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated list at position " + position, error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{ }", "{ \n\t }", " {}", "\n  { }  "})
    void readsEmptyMaps (String text) {

        assertEquals(Map.of(), new JsonReader(text).read());
    }

    @Test
    void readsEmptyMapsInsideLists () {

        assertEquals(List.of(Map.of(), Map.of()), new JsonReader("[{}, {}]").read());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "'{'|0",
            "'{  '|0",
            "' {'|1",
            "'{]'|0",
            "'[{'|1"
    })
    void rejectsUnterminatedMapsNamingWhereTheyStart (String text, int position) {

        JsonReader reader = new JsonReader(text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unterminated map at position " + position, error.getMessage());
    }

    @Test
    void rejectsContentAfterTheMap () {

        JsonReader reader = new JsonReader("{}x");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected content at position 2", error.getMessage());
    }

    @Test
    void closingBraceAloneIsNotAValue () {

        JsonReader reader = new JsonReader("}");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertEquals("Unexpected JSON value at position 0", error.getMessage());
    }

    @Test
    void errorMessageNamesThePosition () {

        JsonReader reader = new JsonReader("nullx");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, reader::read);

        assertTrue(error.getMessage().contains("position 4"));
    }
}
