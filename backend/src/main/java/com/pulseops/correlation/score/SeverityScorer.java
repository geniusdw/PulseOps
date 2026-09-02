package com.pulseops.correlation.score;

import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.SignalEvent;
import org.springframework.stereotype.Component;

/**
 * How much the pair's severities argue for "this matters". Two critical events
 * near each other are a stronger incident signal than two low-severity ones.
 * Score is the summed severity weight normalised by the maximum (CRITICAL +
 * CRITICAL = 8).
 */
@Component
public class SeverityScorer implements PairScorer {

    private static final double MAX_COMBINED_WEIGHT = 8.0;

    private final double weight;

    public SeverityScorer(CorrelationProperties props) {
        this.weight = props.weights().normalized().severity();
    }

    @Override
    public String signal() {
        return "severity";
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public double rawScore(SignalEvent a, SignalEvent b) {
        return (a.severity().weight() + b.severity().weight()) / MAX_COMBINED_WEIGHT;
    }

    @Override
    public String explain(SignalEvent a, SignalEvent b) {
        return "severities " + a.severity() + " + " + b.severity();
    }
}
