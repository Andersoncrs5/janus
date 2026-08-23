package org.janus.shared.domain.database;


import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.result.Result;

public final class DatabaseConstraintHandler {

    private DatabaseConstraintHandler() {
    }

    public static <T> Result<T> handle(
            DataIntegrityViolationException e
    ) {

        Throwable cause = e.getCause();

        String message = cause != null
                ? cause.getMessage()
                : e.getMessage();

        if (message == null) {
            return Result.failure(
                    "Database integrity error",
                    400
            );
        }

        if (message.contains("null value")) {

            String column =
                    DatabaseErrorUtils.extractColumn(message);

            return Result.failure(
                    "Required field '" + column + "' is missing",
                    400
            );
        }

        if (message.contains("value too long")) {

            String column =
                    DatabaseErrorUtils.extractColumn(message);

            return Result.failure(
                    "Field '" + column + "' exceeded the allowed size",
                    400
            );
        }

        return Result.failure(
                "Database integrity error: " + message,
                400
        );
    }
}