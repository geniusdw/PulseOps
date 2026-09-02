package com.pulseops.events.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single stored observation from infrastructure.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Surrogate {@code id} primary key; the public identifier {@code EVT-<id>}
 *       is derived, not stored, to avoid a second unique column.</li>
 *   <li>Indexes on {@code occurred_at}, {@code service} and {@code event_type}
 *       back the filterable {@code GET /api/events} query and the correlation
 *       window scan ({@code occurredAt BETWEEN ?}).</li>
 *   <li>Enums persisted as {@code STRING} so the column is human-readable and
 *       reordering the enum cannot corrupt existing rows.</li>
 *   <li>Effectively immutable after creation — no setters are exposed beyond
 *       what JPA needs; the ingest pipeline builds one via the constructor.</li>
 * </ul>
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_events_service", columnList = "service"),
        @Index(name = "idx_events_event_type", columnList = "event_type")
})
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** When the observation happened (from the producer). */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** When PulseOps received it (server clock) — useful for lag analysis. */
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(nullable = false, length = 64)
    private String service;

    @Column(length = 64)
    private String host;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(length = 64)
    private String metric;

    @Column(name = "metric_value")
    private Double value;

    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * Optional soft reference to a deployment ({@code deployments.id}). Kept as a
     * plain column rather than a {@code @ManyToOne} to keep the ingest write path
     * a single insert with no association loading.
     */
    @Column(name = "deployment_id")
    private Long deploymentId;

    protected EventEntity() {
        // for JPA
    }

    public EventEntity(Instant occurredAt, Instant ingestedAt, String service, String host,
                       EventType eventType, Severity severity, String metric, Double value,
                       String message, Long deploymentId) {
        this.occurredAt = occurredAt;
        this.ingestedAt = ingestedAt;
        this.service = service;
        this.host = host;
        this.eventType = eventType;
        this.severity = severity;
        this.metric = metric;
        this.value = value;
        this.message = message;
        this.deploymentId = deploymentId;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return id == null ? null : "EVT-" + id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public String getService() {
        return service;
    }

    public String getHost() {
        return host;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMetric() {
        return metric;
    }

    public Double getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public Long getDeploymentId() {
        return deploymentId;
    }
}
