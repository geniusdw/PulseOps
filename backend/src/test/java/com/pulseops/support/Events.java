package com.pulseops.support;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;

import java.time.Instant;

/** Concise {@link SignalEvent} builders for tests. */
public final class Events {

    public static final Instant T0 = Instant.parse("2026-09-01T14:30:00Z");

    private static long nextId = 1;

    private Events() {
    }

    public static SignalEvent event(String service, EventType type, Severity severity, Instant at) {
        return new SignalEvent(nextId++, service, type, severity, at, null, null, type + " on " + service);
    }

    public static SignalEvent event(long id, String service, EventType type, Severity severity, Instant at) {
        return new SignalEvent(id, service, type, severity, at, null, null, type + " on " + service);
    }

    public static SignalEvent deploymentEvent(long id, String service, Instant at) {
        return new SignalEvent(id, service, EventType.DEPLOYMENT, Severity.LOW, at, id, null, "deployment");
    }

    public static Instant plusSeconds(long seconds) {
        return T0.plusSeconds(seconds);
    }
}
