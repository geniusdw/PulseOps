package com.pulseops.events;

import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;

import java.time.Instant;

/**
 * Result of validating + normalising an {@code EventRequest}: enums resolved,
 * timestamp defaulted, strings trimmed. This is what the persistence layer
 * turns into an {@code EventEntity}.
 */
public record NormalizedEvent(
        Instant occurredAt,
        String service,
        String host,
        EventType eventType,
        Severity severity,
        String metric,
        Double value,
        String message,
        String deploymentVersion
) {
}
