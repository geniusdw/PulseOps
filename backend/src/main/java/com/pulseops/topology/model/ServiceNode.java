package com.pulseops.topology.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A service in the simulated topology. The service <em>name</em> is the natural
 * primary key — it is short, stable, and is what every event carries, so a
 * surrogate key would only add join noise.
 */
@Entity
@Table(name = "services")
public class ServiceNode {

    /** Service tier, used purely for dashboard layout / grouping. */
    public enum Tier {
        EDGE, APPLICATION, DATA
    }

    @Id
    @Column(length = 64, nullable = false)
    private String name;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Tier tier;

    @Column(length = 256)
    private String description;

    protected ServiceNode() {
    }

    public ServiceNode(String name, String displayName, Tier tier, String description) {
        this.name = name;
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Tier getTier() {
        return tier;
    }

    public String getDescription() {
        return description;
    }
}
