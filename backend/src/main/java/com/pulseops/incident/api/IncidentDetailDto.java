package com.pulseops.incident.api;

import com.pulseops.correlation.model.CorrelationExplanation;
import com.pulseops.events.dto.EventResponse;
import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.timeline.TimelineEntry;

import java.time.Instant;
import java.util.List;

/** Everything the Incident Details page needs, in one response. */
public record IncidentDetailDto(
        String incidentId,
        String title,
        String severity,
        String status,
        Instant startedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt,
        double confidenceScore,
        List<String> affectedServices,
        String probableRootCause,
        Double rootCauseScore,
        String correlationSummary,
        CorrelationExplanation correlation,
        List<RootCauseDto> rootCauses,
        List<TimelineEntry> timeline,
        List<EventResponse> events
) {
    public static IncidentDetailDto of(IncidentEntity i,
                                       List<String> affectedServices,
                                       CorrelationExplanation correlation,
                                       List<RootCauseDto> rootCauses,
                                       List<TimelineEntry> timeline,
                                       List<EventResponse> events) {
        return new IncidentDetailDto(
                i.getPublicId(),
                i.getTitle(),
                i.getSeverity().name(),
                i.getStatus().name(),
                i.getStartedAt(),
                i.getResolvedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                i.getConfidenceScore(),
                affectedServices,
                i.getProbableRootCause(),
                i.getRootCauseScore(),
                i.getCorrelationSummary(),
                correlation,
                rootCauses,
                timeline,
                events);
    }
}
