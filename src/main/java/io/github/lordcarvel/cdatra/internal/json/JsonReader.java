package io.github.lordcarvel.cdatra.internal.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonReader {

    private final String text;

    private int position = 0;

    public JsonReader (String text) {

        if (text == null) {

            throw new IllegalArgumentException("JSON text cannot be null");
        }

        this.text = text;
    }

    public Object read () {

        skipWhitespace();

        Object value = readValue();

        skipWhitespace();

        if (position < text.length()) {

            throw new IllegalArgumentException("Unexpected content at position " + position);
        }

        return value;
    }

    private Object readValue () {

        if (text.startsWith("null", position)) {

            position = position + 4;

            return null;
        }

        if (text.startsWith("true", position)) {

            position = position + 4;

            return true;
        }

        if (text.startsWith("false", position)) {

            position = position + 5;

            return false;
        }

        if (position < text.length() && (text.charAt(position) == '-' || isDigit(text.charAt(position)))) {

            return readNumber();
        }

        if (text.startsWith("\"", position)) {

            return readString();
        }

        if (text.startsWith("[", position)) {

            return readList();
        }

        if (text.startsWith("{", position)) {

            return readMap();
        }

        throw new IllegalArgumentException("Unexpected JSON value at position " + position);
    }

    private List<Object> readList () {

        int start = position;

        position = position + 1;

        List<Object> results = new ArrayList<>();

        skipWhitespace();

        if (position < text.length() && text.charAt(position) == ']') {

            position = position + 1;

            return results;
        }

        while (position < text.length()) {

            results.add(readValue());

            skipWhitespace();

            if (position < text.length() && text.charAt(position) == ']') {

                position = position + 1;

                return results;
            }

            if (position >= text.length()) {

                break;
            }

            if (text.charAt(position) != ',') {

                throw new IllegalArgumentException("Expected ',' or ']' at position " + position);
            }

            position = position + 1;

            skipWhitespace();
        }

        throw new IllegalArgumentException("Unterminated list at position " + start);
    }

    private Map<String, Object> readMap () {

        int start = position;

        position = position + 1;

        Map<String, Object> results = new LinkedHashMap<>();

        skipWhitespace();

        if (position < text.length() && text.charAt(position) == '}') {

            position = position + 1;

            return results;
        }

        throw new IllegalArgumentException("Unterminated map at position " + start);
    }

    private boolean isWhitespace (char c) {

        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private void skipWhitespace () {

        while (position < text.length() && isWhitespace(text.charAt(position))) {

            position = position + 1;
        }
    }

    private boolean isDigit (char c) {

        return c >= '0' && c <= '9';
    }

    private Object readNumber () {

        int start = position;

        if (text.charAt(position) == '-') {

            position = position + 1;
        }

        if (position + 1 < text.length() && text.charAt(position) == '0' && isDigit(text.charAt(position + 1))) {

            throw new IllegalArgumentException("Leading zeros are not allowed at position " + position);
        }

        requireDigits();

        boolean isDecimal = false;

        if (position < text.length() && text.charAt(position) == '.') {

            isDecimal = true;

            position = position + 1;

            requireDigits();
        }

        if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {

            isDecimal = true;

            position = position + 1;

            if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) {

                position = position + 1;
            }

            requireDigits();
        }

        String number = text.substring(start, position);

        if (isDecimal) {

            double value = Double.parseDouble(number);

            if (!Double.isFinite(value)) {

                throw new IllegalArgumentException("Number out of range at position " + start);
            }

            return value;
        }

        try {

            return Long.parseLong(number);
        } catch (NumberFormatException e) {

            throw new IllegalArgumentException("Number out of range at position " + start);
        }
    }

    private void skipDigits () {

        while (position < text.length() && isDigit(text.charAt(position))) {

            position = position + 1;
        }
    }

    private void requireDigits () {

        int start = position;

        skipDigits();

        if (position == start) {

            throw new IllegalArgumentException("Expected a digit at position " + position);
        }
    }

    private String readString () {

        int start = position;

        position = position + 1;

        StringBuilder result = new StringBuilder();

        while (position < text.length()) {

            char c = text.charAt(position);

            if (c == '"') {

                position = position + 1;

                return result.toString();
            }

            if (c == '\\') {

                position = position + 1;

                if (position >= text.length()) {

                    throw new IllegalArgumentException("Unterminated string at position " + start);
                }

                if (text.charAt(position) == 'u') {

                    position = position + 1;

                    result.append(readUnicodeEscape());

                    continue;
                }

                result.append(unescape(text.charAt(position)));

                position = position + 1;

                continue;
            }

            if (isControl(c)) {

                throw new IllegalArgumentException("Invalid control character at position " + position);
            }

            result.append(c);

            position = position + 1;
        }

        throw new IllegalArgumentException("Unterminated string at position " + start);
    }

    private char unescape (char c) {

        if (c == '"' || c == '\\' || c == '/') {

            return c;
        }

        if (c == 'b') {

            return '\b';
        }

        if (c == 'f') {

            return '\f';
        }

        if (c == 'n') {

            return '\n';
        }

        if (c == 'r') {

            return '\r';
        }

        if (c == 't') {

            return '\t';
        }

        throw new IllegalArgumentException("Invalid escape at position " + position);
    }

    private boolean isControl (char c) {

        return c < 32;
    }

    private char readUnicodeEscape () {

        int value = 0;

        for (int digit = 0; digit < 4; digit++) {

            if (position >= text.length()) {

                throw new IllegalArgumentException("Invalid unicode escape at position " + position);
            }

            value = value * 16 + hexValue(text.charAt(position));

            position = position + 1;
        }

        return (char) value;
    }

    private int hexValue (char c) {

        if (isDigit(c)) {

            return c - '0';
        }

        if (c >= 'a' && c <= 'f') {

            return c - 'a' + 10;
        }

        if (c >= 'A' && c <= 'F') {

            return c - 'A' + 10;
        }

        throw new IllegalArgumentException("Invalid unicode escape at position " + position);
    }
}