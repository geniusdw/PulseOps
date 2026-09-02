package com.pulseops.incident.api;

import com.pulseops.incident.model.IncidentEntity;

import java.time.Instant;
import java.util.List;

/** Row shape for the Incidents table. */
public record IncidentSummaryDto(
        String incidentId,
        String title,
        String severity,
        String status,
        List<String> affectedServices,
        Instant startedAt,
        Instant updatedAt,
        double confidenceScore,
        String probableRootCause,
        Double rootCauseScore,
        int eventCount
) {
    public static IncidentSummaryDto of(IncidentEntity i, List<String> affectedServices, int eventCount) {
        return new IncidentSummaryDto(
                i.getPublicId(),
                i.getTitle(),
                i.getSeverity().name(),
                i.getStatus().name(),
                affectedServices,
                i.getStartedAt(),
                i.getUpdatedAt(),
                i.getConfidenceScore(),
                i.getProbableRootCause(),
                i.getRootCauseScore(),
                eventCount);
    }
}
