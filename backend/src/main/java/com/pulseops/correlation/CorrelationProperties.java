package com.pulseops.correlation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding of {@code pulseops.correlation.*}. The correlation model is
 * fully described by this record — there are no magic numbers in the engine.
 *
 * @param windowMinutes size of the sliding window in which events are candidates
 * @param threshold     pairwise score at/above which two events are "linked"
 * @param weights       relative importance of each sub-score (need not sum to 1;
 *                      they are normalised at load time)
 * @param temporal      temporal sub-score tuning
 * @param deployment    deployment sub-score tuning
 */
@ConfigurationProperties(prefix = "pulseops.correlation")
public record CorrelationProperties(
        int windowMinutes,
        double threshold,
        Weights weights,
        Temporal temporal,
        Deployment deployment
) {

    public record Weights(
            double temporal,
            double serviceDependency,
            double eventType,
            double deployment,
            double severity
    ) {
        public double sum() {
            return temporal + serviceDependency + eventType + deployment + severity;
        }

        /** Weights rescaled to sum to 1, so the final score stays in [0,1]. */
        public Weights normalized() {
            double s = sum();
            return new Weights(temporal / s, serviceDependency / s, eventType / s,
                    deployment / s, severity / s);
        }
    }

    public record Temporal(long halfLifeSeconds) {
    }

    public record Deployment(long lookbackMinutes) {
    }

    public CorrelationProperties {
        if (windowMinutes < 1) throw new IllegalArgumentException("windowMinutes must be >= 1");
        if (threshold < 0 || threshold > 1) throw new IllegalArgumentException("threshold must be in [0,1]");
        if (weights == null || weights.sum() <= 0) throw new IllegalArgumentException("weights must sum to > 0");
    }
}
