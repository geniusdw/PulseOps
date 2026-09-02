package com.pulseops.common.error;

import java.time.Instant;
import java.util.List;

/**
 * The single JSON error shape returned by every endpoint. Stack traces are
 * never serialised — only a stable machine-readable {@code error} code and a
 * human {@code message}.
 *
 * <pre>
 * {
 *   "timestamp": "2026-09-01T14:32:11Z",
 *   "status": 404,
 *   "error": "INCIDENT_NOT_FOUND",
 *   "message": "Incident INC-1829 was not found",
 *   "details": []
 * }
 * </pre>
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
