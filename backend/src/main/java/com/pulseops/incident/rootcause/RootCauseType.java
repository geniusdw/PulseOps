package com.pulseops.incident.rootcause;

/** Candidate categories the root-cause ranker considers. */
public enum RootCauseType {
    DEPLOYMENT("Deployment"),
    DATABASE_FAILURE("Database failure"),
    RESOURCE_EXHAUSTION("Resource exhaustion"),
    NETWORK_PROBLEM("Network problem"),
    SERVICE_FAILURE("Service failure"),
    DEPENDENCY_FAILURE("Upstream dependency failure");

    private final String label;

    RootCauseType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
