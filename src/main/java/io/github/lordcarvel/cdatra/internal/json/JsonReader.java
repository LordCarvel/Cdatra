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

        while (position < text.length() && isDigit(text.charAt(position))) {

            position = position + 1;
        }

        return Long.parseLong(text.substring(start, position));
    }
}