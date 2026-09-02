package com.pulseops.events.dto;

import com.pulseops.events.model.EventEntity;

import java.time.Instant;

/** Outgoing representation of a stored event. */
public record EventResponse(
        String eventId,
        Instant timestamp,
        Instant ingestedAt,
        String service,
        String host,
        String eventType,
        String severity,
        String metric,
        Double value,
        String message,
        String deploymentId
) {
    public static EventResponse from(EventEntity e) {
        return new EventResponse(
                e.getPublicId(),
                e.getOccurredAt(),
                e.getIngestedAt(),
                e.getService(),
                e.getHost(),
                e.getEventType().name(),
                e.getSeverity().name(),
                e.getMetric(),
                e.getValue(),
                e.getMessage(),
                e.getDeploymentId() == null ? null : "DEP-" + e.getDeploymentId()
        );
    }
}
