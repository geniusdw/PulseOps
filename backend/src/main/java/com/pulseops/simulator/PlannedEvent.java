package com.pulseops.simulator;

import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;

/**
 * A single event in a scenario script, positioned by seconds-after-start.
 *
 * @param offsetSeconds  when it fires relative to the scenario start
 * @param service        emitting service
 * @param type           event type
 * @param severity       severity
 * @param metric         optional metric name
 * @param value          optional metric value
 * @param message        human message
 * @param linkToDeployment if true, tag this event with the scenario's deployment id
 * @param version        deployment version (only for DEPLOYMENT events)
 */
public record PlannedEvent(
        long offsetSeconds,
        String service,
        EventType type,
        Severity severity,
        String metric,
        Double value,
        String message,
        boolean linkToDeployment,
        String version
) {
    static PlannedEvent of(long offsetSeconds, String service, EventType type, Severity severity,
                           String metric, Double value, String message) {
        return new PlannedEvent(offsetSeconds, service, type, severity, metric, value, message, false, null);
    }

    static PlannedEvent linked(long offsetSeconds, String service, EventType type, Severity severity,
                               String metric, Double value, String message) {
        return new PlannedEvent(offsetSeconds, service, type, severity, metric, value, message, true, null);
    }

    static PlannedEvent deployment(long offsetSeconds, String service, String version, String message) {
        return new PlannedEvent(offsetSeconds, service, EventType.DEPLOYMENT, Severity.LOW,
                null, null, message, false, version);
    }
}
