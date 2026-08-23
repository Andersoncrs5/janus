package org.janus.shared.domain.database;


public final class DatabaseErrorUtils {

    private DatabaseErrorUtils() {
    }

    public static String extractColumn(String message) {

        if (message == null || message.isBlank()) {
            return "unknown";
        }

        int columnStart = message.indexOf("column \"");

        if (columnStart >= 0) {

            columnStart += "column \"".length();

            int columnEnd = message.indexOf("\"", columnStart);

            if (columnEnd >= 0) {
                return message.substring(columnStart, columnEnd);
            }
        }

        return "unknown";
    }
}