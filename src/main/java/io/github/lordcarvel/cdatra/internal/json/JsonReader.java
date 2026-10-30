package io.github.lordcarvel.cdatra.internal.json;

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

        throw new IllegalArgumentException("Unexpected JSON value at position " + position);
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

            result.append(c);

            position = position + 1;
        }

        throw new IllegalArgumentException("Unterminated string at position " + start);
    }
}