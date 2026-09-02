package com.pulseops.events.model;

/**
 * Event / incident severity. Ordinal order matters: higher ordinal == more
 * severe, so {@code Severity.compareTo} and {@code max()} are meaningful.
 */
public enum Severity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    /** Numeric weight used by the severity sub-score and incident severity calc. */
    public int weight() {
        return weight;
    }

    public static Severity max(Severity a, Severity b) {
        return a.weight >= b.weight ? a : b;
    }
}
