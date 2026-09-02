package com.pulseops.correlation.score;

import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.SignalEvent;
import org.springframework.stereotype.Component;

/**
 * Whether the two event types form a known failure pattern
 * (e.g. {@code DB_CONNECTION_EXHAUSTION} + {@code HTTP_500}). Delegates to the
 * curated {@link EventTypeAffinity} table.
 */
@Component
public class EventTypeScorer implements PairScorer {

    private final double weight;
    private final EventTypeAffinity affinity;

    public EventTypeScorer(CorrelationProperties props, EventTypeAffinity affinity) {
        this.weight = props.weights().normalized().eventType();
        this.affinity = affinity;
    }

    @Override
    public String signal() {
        return "event-type";
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public double rawScore(SignalEvent a, SignalEvent b) {
        return affinity.between(a.eventType(), b.eventType());
    }

    @Override
    public String explain(SignalEvent a, SignalEvent b) {
        String pair = a.eventType() + " + " + b.eventType();
        return affinity.isStrongPattern(a.eventType(), b.eventType())
                ? pair + " is a known failure pattern"
                : pair;
    }
}
