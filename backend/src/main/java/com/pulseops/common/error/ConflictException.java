package com.pulseops.common.error;

/**
 * Thrown when an operation conflicts with current state — e.g. resolving an
 * incident that is already RESOLVED. Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    private final String errorCode;

    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
