package com.pulseops.common.error;

import java.util.List;

/**
 * Thrown by domain validators (e.g. {@code EventValidator}) when a request is
 * syntactically valid JSON but semantically invalid — an unknown service name,
 * a future timestamp, an unsupported event type. Mapped to HTTP 400.
 */
public class ValidationException extends RuntimeException {

    private final List<String> details;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
