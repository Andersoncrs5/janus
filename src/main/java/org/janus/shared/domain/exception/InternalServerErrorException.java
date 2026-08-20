package org.janus.shared.domain.exception;

public class InternalServerErrorException extends RuntimeException {
    private Throwable exception;
    private int statusCode;

    public InternalServerErrorException(String message) {
        super(message);
    }

    public InternalServerErrorException(String message, Throwable exception) {
        super(message);
        this.exception = exception;
    }

    public InternalServerErrorException(String message, Throwable exception, int statusCode) {
        super(message);
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