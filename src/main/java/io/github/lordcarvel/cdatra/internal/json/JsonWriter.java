package io.github.lordcarvel.cdatra.internal.json;

import java.util.List;
import java.util.Map;

public class JsonWriter {

    public String write (Object value) {

        StringBuilder json = new StringBuilder();

        writeValue(json, value);

        return json.toString();
    }

    private void writeValue (StringBuilder json, Object value) {

        if (value == null) {

            json.append("null");
        } else if (value instanceof Boolean) {

            json.append(value);
        } else if (value instanceof Number) {

            json.append(value);
        } else if (value instanceof String) {

            writeString(json, (String) value);
        } else if (value instanceof List) {

            writeList(json, (List<?>) value);
        } else if (value instanceof Map) {

            writeMap(json, (Map<?, ?>) value);
        } else {

            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
        }
    }

    private void writeString (StringBuilder json, String text) {

        json.append('"');

        for (int i = 0; i < text.length(); ) {

            char c = text.charAt(i);

            if (c == '"') {

                json.append("\\\"");
            } else if (c == '\\') {

                json.append("\\\\");
            } else if (c < 32) {

                json.append(String.format("\\u%04x", (int) c));
            } else {

                json.append(c);
            }

            i = i + 1;
        }

        json.append('"');
    }

    private void writeList (StringBuilder json, List<?> list) {

        json.append('[');

        for (int i = 0; i < list.size(); ) {

            if (i > 0) {

                json.append(',');
            }

            writeValue(json, list.get(i));

            i = i + 1;
        }

        json.append(']');
    }

    private void writeMap (StringBuilder json, Map<?, ?> map) {

        json.append('{');

        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {

            if (!(entry.getKey() instanceof String)) {

                throw new IllegalArgumentException("JSON object keys must be strings");
            }

            if (!first) {

                json.append(',');
            }

            first = false;

            writeString(json, (String) entry.getKey());
            json.append(':');
            writeValue(json, entry.getValue());
        }

        json.append('}');
    }
}