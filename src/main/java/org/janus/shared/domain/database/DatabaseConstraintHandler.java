package org.janus.shared.domain.database;

import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.result.Result;

public final class DatabaseConstraintHandler {

    private DatabaseConstraintHandler() {
    }

    public static <T> Result<T> handle(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        String message = cause != null && cause.getMessage() != null
                ? cause.getMessage()
                : e.getMessage();

        if (message == null) {
            return Result.failure("Database integrity error", 400);
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("null value")) {
            String column = DatabaseErrorUtils.extractColumn(message);
            return Result.failure("Required field '" + column + "' is missing", 400);
        }

        if (lowerMessage.contains("value too long")) {
            String column = DatabaseErrorUtils.extractColumn(message);
            return Result.failure("Field '" + column + "' exceeded the allowed size", 400);
        }

        if (lowerMessage.contains("foreign key constraint") || lowerMessage.contains("is still referenced")) {
            return Result.failure("Referenced resource does not exist or is currently in use", 409);
        }

        if (lowerMessage.contains("unique constraint") || lowerMessage.contains("already exists")) {
            return Result.failure("A record with this unique value already exists", 409);
        }

        if (lowerMessage.contains("violates check constraint")) {
            return Result.failure("Operation violated database validation rules", 400);
        }

        if (lowerMessage.contains("out of range") || lowerMessage.contains("invalid input syntax")) {
            return Result.failure("Invalid data format or value out of allowed range", 400);
        }

        return Result.failure("Database integrity error: " + message, 400);
    }
}