package com.pulseops.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Incoming event payload for {@code POST /api/events}.
 *
 * <p>{@code eventType} and {@code severity} are accepted as free-form strings
 * rather than enums so that an unknown value yields a precise
 * {@code VALIDATION_FAILED} message ("unknown event type 'HTTP_418'") instead of
 * an opaque JSON parse error. The mapping to enums happens in
 * {@code EventValidator}.
 */
public record EventRequest(

        /** When the observation occurred. Optional; defaults to server time if absent. */
        Instant timestamp,

        @NotBlank(message = "service is required")
        @Size(max = 64)
        String service,

        @Size(max = 64)
        String host,

        @NotBlank(message = "eventType is required")
        String eventType,

        @NotBlank(message = "severity is required")
        String severity,

        @Size(max = 64)
        String metric,

        Double value,

        @NotBlank(message = "message is required")
        @Size(max = 1000)
        String message,

        /** Only meaningful when eventType == DEPLOYMENT. */
        @Size(max = 64)
        String deploymentVersion
) {
}
