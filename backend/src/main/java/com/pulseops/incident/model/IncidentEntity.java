package com.pulseops.incident.model;

import com.pulseops.events.model.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * An incident: a group of correlated events plus the engine's derived
 * assessment (severity, confidence, probable root cause).
 *
 * <p>Affected services are intentionally <em>not</em> stored — they are derived
 * from the linked events so they can never drift. The {@code @Version} column
 * gives optimistic locking in case the reconciliation lock is ever relaxed.
 */
@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_started_at", columnList = "started_at")
})
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Confidence that the linked events truly belong together (cluster strength). */
    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "probable_root_cause", length = 128)
    private String probableRootCause;

    @Column(name = "root_cause_score")
    private Double rootCauseScore;

    /** The one-line correlation explanation, refreshed on each reconciliation. */
    @Column(name = "correlation_summary", length = 2000)
    private String correlationSummary;

    @Version
    private Long version;

    protected IncidentEntity() {
    }

    public IncidentEntity(String title, Severity severity, Instant startedAt, Instant now) {
        this.title = title;
        this.severity = severity;
        this.status = IncidentStatus.OPEN;
        this.startedAt = startedAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.confidenceScore = 0.0;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return id == null ? null : "INC-" + id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getProbableRootCause() {
        return probableRootCause;
    }

    public void setProbableRootCause(String probableRootCause) {
        this.probableRootCause = probableRootCause;
    }

    public Double getRootCauseScore() {
        return rootCauseScore;
    }

    public void setRootCauseScore(Double rootCauseScore) {
        this.rootCauseScore = rootCauseScore;
    }

    public String getCorrelationSummary() {
        return correlationSummary;
    }

    public void setCorrelationSummary(String correlationSummary) {
        this.correlationSummary = correlationSummary;
    }
}
