package com.pulseops.correlation.score;

import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.SignalEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Closeness in time. Uses exponential decay with a configurable half-life: two
 * events {@code halfLifeSeconds} apart score 0.5, twice that 0.25, and so on.
 * Exponential (rather than linear) decay matches intuition — events seconds
 * apart are far more suspicious than events that are 4 vs 5 minutes apart.
 */
@Component
public class TemporalScorer implements PairScorer {

    private final double weight;
    private final double halfLifeSeconds;

    public TemporalScorer(CorrelationProperties props) {
        this.weight = props.weights().normalized().temporal();
        this.halfLifeSeconds = props.temporal().halfLifeSeconds();
    }

    @Override
    public String signal() {
        return "temporal";
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public double rawScore(SignalEvent a, SignalEvent b) {
        double gapSeconds = Math.abs(Duration.between(a.occurredAt(), b.occurredAt()).toMillis()) / 1000.0;
        return Math.pow(0.5, gapSeconds / halfLifeSeconds);
    }

    @Override
    public String explain(SignalEvent a, SignalEvent b) {
        long gap = Math.abs(Duration.between(a.occurredAt(), b.occurredAt()).toSeconds());
        return gap + "s apart";
    }
}
