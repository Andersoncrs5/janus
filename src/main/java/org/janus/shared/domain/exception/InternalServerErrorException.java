package org.janus.shared.domain.exception;

public class InternalServerErrorException extends RuntimeException {
    private final Throwable exception;
    private final int statusCode;

    public InternalServerErrorException(String message) {
        super(message);
        this.exception = null;
        this.statusCode = 500;
    }

    public InternalServerErrorException(String message, Throwable exception) {
        super(message, exception);
        this.exception = exception;
        this.statusCode = 500;
    }

    public InternalServerErrorException(String message, Throwable exception, int statusCode) {
        super(message, exception);
        this.exception = exception;
        this.statusCode = statusCode;
    }

    public Throwable getException() {
        return exception;
    }

    public int getStatusCode() {
        return statusCode;
    }
}