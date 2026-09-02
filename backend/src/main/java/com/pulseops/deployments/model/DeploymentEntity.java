package com.pulseops.deployments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A recorded deployment. Created whenever a {@code DEPLOYMENT} event is ingested,
 * so the correlation engine and root-cause ranker can ask "was there a
 * deployment to a related service shortly before this anomaly?".
 */
@Entity
@Table(name = "deployments", indexes = {
        @Index(name = "idx_deployments_deployed_at", columnList = "deployed_at"),
        @Index(name = "idx_deployments_service", columnList = "service")
})
public class DeploymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String service;

    @Column(length = 64)
    private String version;

    @Column(name = "deployed_at", nullable = false)
    private Instant deployedAt;

    protected DeploymentEntity() {
    }

    public DeploymentEntity(String service, String version, Instant deployedAt) {
        this.service = service;
        this.version = version;
        this.deployedAt = deployedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return id == null ? null : "DEP-" + id;
    }

    public String getService() {
        return service;
    }

    public String getVersion() {
        return version;
    }

    public Instant getDeployedAt() {
        return deployedAt;
    }
}
