package com.pulseops.correlation.model;

import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;

import java.time.Instant;

/**
 * Immutable view of a stored event, used by the correlation / incident / root
 * cause / timeline logic.
 *
 * <p>Why a separate type instead of passing {@link EventEntity} around: the
 * correlation engine is pure domain logic with no dependency on JPA, Spring or
 * HTTP. It can be unit tested by constructing {@code SignalEvent} records
 * directly, with no database and no Spring context.
 */
public record SignalEvent(
        long id,
        String service,
        EventType eventType,
        Severity severity,
        Instant occurredAt,
        Long deploymentId,
        Double value,
        String message
) {
    public String publicId() {
        return "EVT-" + id;
    }

    public static SignalEvent from(EventEntity e) {
        return new SignalEvent(
                e.getId(),
                e.getService(),
                e.getEventType(),
                e.getSeverity(),
                e.getOccurredAt(),
                e.getDeploymentId(),
                e.getValue(),
                e.getMessage()
        );
    }
}
