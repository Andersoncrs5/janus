package org.janus.configs.mapperStruct.extensions;

public class StringUtils {

    public static String trimToNull(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String toUpperCase(String input) {
        return input == null ? null : input.toUpperCase();
    }
}
