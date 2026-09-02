package com.pulseops.correlation.score;

import com.pulseops.events.model.EventType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.pulseops.events.model.EventType.CPU_SPIKE;
import static com.pulseops.events.model.EventType.DB_CONNECTION_EXHAUSTION;
import static com.pulseops.events.model.EventType.DEPLOYMENT;
import static com.pulseops.events.model.EventType.HIGH_LATENCY;
import static com.pulseops.events.model.EventType.HTTP_500;
import static com.pulseops.events.model.EventType.HTTP_503;
import static com.pulseops.events.model.EventType.MEMORY_SPIKE;
import static com.pulseops.events.model.EventType.NETWORK_ERROR;
import static com.pulseops.events.model.EventType.PAYMENT_FAILURE;
import static com.pulseops.events.model.EventType.QUEUE_BACKLOG;
import static com.pulseops.events.model.EventType.SERVICE_RESTART;

/**
 * Domain knowledge, expressed as data: how strongly a pair of event types
 * suggests a shared underlying cause. Values are in {@code [0,1]}.
 *
 * <p>This is a curated heuristic table, not a learned model. In production the
 * pairs and weights would be tuned from historical incidents (or learned), but
 * the shape of the interface would not change.
 */
@Component
public class EventTypeAffinity {

    private static final double DEFAULT_UNRELATED = 0.15;
    private static final double SAME_TYPE = 0.55;

    /** Symmetric map keyed by an unordered pair. */
    private final Map<PairKey, Double> affinity = new HashMap<>();

    public EventTypeAffinity() {
        // Classic resource-exhaustion cascade
        put(DB_CONNECTION_EXHAUSTION, HIGH_LATENCY, 0.9);
        put(DB_CONNECTION_EXHAUSTION, HTTP_500, 0.9);
        put(DB_CONNECTION_EXHAUSTION, PAYMENT_FAILURE, 0.85);
        put(HIGH_LATENCY, HTTP_500, 0.8);
        put(HIGH_LATENCY, PAYMENT_FAILURE, 0.7);
        put(HTTP_500, PAYMENT_FAILURE, 0.8);
        put(QUEUE_BACKLOG, HIGH_LATENCY, 0.75);
        put(QUEUE_BACKLOG, DB_CONNECTION_EXHAUSTION, 0.65);

        // Bad deployment signature
        put(DEPLOYMENT, HIGH_LATENCY, 0.75);
        put(DEPLOYMENT, HTTP_500, 0.8);
        put(DEPLOYMENT, SERVICE_RESTART, 0.85);
        put(DEPLOYMENT, PAYMENT_FAILURE, 0.6);
        put(SERVICE_RESTART, HTTP_503, 0.7);
        put(SERVICE_RESTART, HIGH_LATENCY, 0.6);

        // CPU / memory saturation
        put(CPU_SPIKE, HIGH_LATENCY, 0.85);
        put(CPU_SPIKE, HTTP_503, 0.8);
        put(CPU_SPIKE, MEMORY_SPIKE, 0.6);
        put(MEMORY_SPIKE, HIGH_LATENCY, 0.8);
        put(MEMORY_SPIKE, SERVICE_RESTART, 0.7);
        put(MEMORY_SPIKE, HTTP_503, 0.7);

        // Network failure signature
        put(NETWORK_ERROR, HTTP_503, 0.85);
        put(NETWORK_ERROR, HIGH_LATENCY, 0.75);
        put(NETWORK_ERROR, SERVICE_RESTART, 0.5);
        put(NETWORK_ERROR, DB_CONNECTION_EXHAUSTION, 0.55);

        // Error-code siblings
        put(HTTP_500, HTTP_503, 0.6);
    }

    private void put(EventType a, EventType b, double value) {
        affinity.put(new PairKey(a, b), value);
    }

    /** Symmetric lookup; unknown pairs fall back to a low baseline. */
    public double between(EventType a, EventType b) {
        if (a == b) {
            return SAME_TYPE;
        }
        return affinity.getOrDefault(new PairKey(a, b), DEFAULT_UNRELATED);
    }

    /** Pairs at or above this affinity are worth calling out in explanations. */
    public boolean isStrongPattern(EventType a, EventType b) {
        return between(a, b) >= 0.7;
    }

    /** Order-independent key for a pair of event types. */
    private record PairKey(Set<EventType> members) {
        PairKey(EventType a, EventType b) {
            this(a == b ? Set.of(a) : Set.of(a, b));
        }
    }
}
