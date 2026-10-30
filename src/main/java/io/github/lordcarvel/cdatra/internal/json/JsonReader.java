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

        Object value = readValue();

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

        throw new IllegalArgumentException("Unexpected JSON value at position " + position);
    }
}