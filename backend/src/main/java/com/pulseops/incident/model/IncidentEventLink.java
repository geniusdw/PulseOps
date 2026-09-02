package com.pulseops.incident.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The many-to-many association between incidents and events, modelled as an
 * entity because the link carries data: the {@code correlationScore} that
 * attached this event to this incident.
 *
 * <p>Plain {@code Long} id columns rather than {@code @ManyToOne} associations —
 * the reconciliation code works with ids and never needs to navigate the object
 * graph, so associations would only add lazy-loading overhead. The
 * {@code (incident_id, event_id)} unique constraint makes attaching idempotent.
 */
@Entity
@Table(name = "incident_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_incident_event", columnNames = {"incident_id", "event_id"}),
        indexes = {
                @Index(name = "idx_incident_events_incident", columnList = "incident_id"),
                @Index(name = "idx_incident_events_event", columnList = "event_id")
        })
public class IncidentEventLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "correlation_score", nullable = false)
    private double correlationScore;

    protected IncidentEventLink() {
    }

    public IncidentEventLink(Long incidentId, Long eventId, double correlationScore) {
        this.incidentId = incidentId;
        this.eventId = eventId;
        this.correlationScore = correlationScore;
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getEventId() {
        return eventId;
    }

    public double getCorrelationScore() {
        return correlationScore;
    }

    public void setCorrelationScore(double correlationScore) {
        this.correlationScore = correlationScore;
    }
}
