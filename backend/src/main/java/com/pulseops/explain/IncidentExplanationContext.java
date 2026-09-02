package com.pulseops.explain;

import com.pulseops.incident.api.RootCauseDto;
import com.pulseops.incident.timeline.TimelineEntry;

import java.util.List;

/**
 * The deterministic, structured summary of an incident handed to an explainer.
 * This — not raw logs — is the only input. The explainer rephrases; it does not
 * decide grouping or root cause.
 */
public record IncidentExplanationContext(
        String incidentId,
        String title,
        String severity,
        String status,
        List<String> affectedServices,
        String correlationSummary,
        List<RootCauseDto> rootCauses,
        List<TimelineEntry> timeline
) {
}
