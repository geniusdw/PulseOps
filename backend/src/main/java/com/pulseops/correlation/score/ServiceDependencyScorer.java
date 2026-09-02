package com.pulseops.correlation.score;

import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.topology.TopologyService;
import org.springframework.stereotype.Component;

/**
 * How close two events' services sit in the dependency graph. Events on the same
 * service, or on services one hop apart (a caller and its dependency), are much
 * more likely to share a cause than events on unrelated services.
 *
 * <p>Score decays geometrically with hop distance: {@code 0.6 ^ distance}
 * (distance 0 -> 1.0, 1 -> 0.6, 2 -> 0.36, ...). Unknown or unreachable pairs
 * score 0.
 */
@Component
public class ServiceDependencyScorer implements PairScorer {

    private static final double DECAY_PER_HOP = 0.6;

    private final double weight;
    private final TopologyService topology;

    public ServiceDependencyScorer(CorrelationProperties props, TopologyService topology) {
        this.weight = props.weights().normalized().serviceDependency();
        this.topology = topology;
    }

    @Override
    public String signal() {
        return "service-dependency";
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public double rawScore(SignalEvent a, SignalEvent b) {
        int distance = topology.dependencyDistance(a.service(), b.service());
        if (distance < 0) {
            return 0.0;
        }
        return Math.pow(DECAY_PER_HOP, distance);
    }

    @Override
    public String explain(SignalEvent a, SignalEvent b) {
        int distance = topology.dependencyDistance(a.service(), b.service());
        if (distance == 0) {
            return "same service (" + a.service() + ")";
        }
        if (distance == 1) {
            return a.service() + " and " + b.service() + " are directly dependent";
        }
        if (distance > 1) {
            return a.service() + " and " + b.service() + " are " + distance + " dependency hops apart";
        }
        return a.service() + " and " + b.service() + " have no known dependency path";
    }
}
