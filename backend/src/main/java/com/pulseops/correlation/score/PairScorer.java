package com.pulseops.correlation.score;

import com.pulseops.correlation.model.SignalEvent;

/**
 * Scores one dimension of similarity between two events. Implementations are
 * stateless and independently unit-testable. Adding a new correlation signal
 * means adding one {@code PairScorer} bean — the aggregator picks it up.
 */
public interface PairScorer {

    /** Stable signal name, also used as the weight key. */
    String signal();

    /** The configured, normalised weight for this signal in {@code [0,1]}. */
    double weight();

    /** Raw strength of this signal for the pair, in {@code [0,1]}. */
    double rawScore(SignalEvent a, SignalEvent b);

    /** Human-readable justification for the raw score (for explanations). */
    String explain(SignalEvent a, SignalEvent b);

    default SubScore score(SignalEvent a, SignalEvent b) {
        double raw = clamp(rawScore(a, b));
        return new SubScore(signal(), raw, weight(), explain(a, b));
    }

    static double clamp(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
