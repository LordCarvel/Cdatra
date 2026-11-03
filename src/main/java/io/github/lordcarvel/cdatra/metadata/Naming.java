package io.github.lordcarvel.cdatra.metadata;

public class Naming {

    public static String toSnakeCase (String name) {

        if (name == null || name.isEmpty()) {

            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < name.length(); ) {

            char c = name.charAt(i);

            if (Character.isUpperCase(c)) {

                if (i > 0) {

                    result.append("_");
                }

                result.append(Character.toLowerCase(c));
            } else {

                result.append(c);
            }

            i = i + 1;
        }

        return result.toString();
    }
}
