package com.pulseops.incident.model;

/**
 * Incident lifecycle. Transitions are enforced by {@code IncidentService}:
 * <pre>
 *   OPEN ──acknowledge──▶ INVESTIGATING ──resolve──▶ RESOLVED
 *     └────────────────── resolve ──────────────────────▶ RESOLVED
 * </pre>
 * A RESOLVED incident is terminal; new correlated activity starts a fresh one.
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED
}
