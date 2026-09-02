package com.pulseops.events.model;

/**
 * The observation types PulseOps understands. Kept as a closed enum so the
 * ingestion layer can reject unknown types and the correlation layer can hold a
 * static affinity table keyed by pairs of these values.
 */
public enum EventType {
    HTTP_500(Category.ERROR),
    HTTP_503(Category.ERROR),
    HIGH_LATENCY(Category.PERFORMANCE),
    CPU_SPIKE(Category.RESOURCE),
    MEMORY_SPIKE(Category.RESOURCE),
    DB_CONNECTION_EXHAUSTION(Category.RESOURCE),
    NETWORK_ERROR(Category.NETWORK),
    DEPLOYMENT(Category.CHANGE),
    SERVICE_RESTART(Category.CHANGE),
    PAYMENT_FAILURE(Category.BUSINESS),
    QUEUE_BACKLOG(Category.PERFORMANCE);

    /** Coarse grouping used by the event-type affinity heuristic. */
    public enum Category {
        ERROR, PERFORMANCE, RESOURCE, NETWORK, CHANGE, BUSINESS
    }

    private final Category category;

    EventType(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }
}
