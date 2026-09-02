package com.pulseops.incident.timeline;

import java.time.Instant;

/**
 * One chronological entry in an incident timeline.
 *
 * @param at    when it happened
 * @param kind  EVENT | DEPLOYMENT | INCIDENT
 * @param title short label
 * @param detail longer description
 * @param severity severity for EVENT entries, else null
 * @param refId  EVT-/DEP-/INC- id this entry points at
 */
public record TimelineEntry(
        Instant at,
        String kind,
        String title,
        String detail,
        String severity,
        String refId
) {
}
