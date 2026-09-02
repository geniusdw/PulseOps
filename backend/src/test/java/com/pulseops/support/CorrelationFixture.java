package com.pulseops.support;

import com.pulseops.correlation.CorrelationEngine;
import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.CorrelationScorer;
import com.pulseops.correlation.score.DeploymentProximityScorer;
import com.pulseops.correlation.score.EventTypeAffinity;
import com.pulseops.correlation.score.EventTypeScorer;
import com.pulseops.correlation.score.PairScorer;
import com.pulseops.correlation.score.ServiceDependencyScorer;
import com.pulseops.correlation.score.SeverityScorer;
import com.pulseops.correlation.score.TemporalScorer;
import com.pulseops.topology.TopologyService;

import java.util.List;

/** Wires the real correlation stack with production-default weights for tests. */
public final class CorrelationFixture {

    private CorrelationFixture() {
    }

    public static CorrelationProperties defaultProperties() {
        return new CorrelationProperties(
                10,
                0.55,
                new CorrelationProperties.Weights(0.30, 0.25, 0.20, 0.15, 0.10),
                new CorrelationProperties.Temporal(120),
                new CorrelationProperties.Deployment(15));
    }

    public static CorrelationScorer scorer(CorrelationProperties props, TopologyService topology) {
        List<PairScorer> scorers = List.of(
                new TemporalScorer(props),
                new ServiceDependencyScorer(props, topology),
                new EventTypeScorer(props, new EventTypeAffinity()),
                new DeploymentProximityScorer(props),
                new SeverityScorer(props));
        return new CorrelationScorer(scorers, props);
    }

    public static CorrelationEngine engine(CorrelationProperties props, TopologyService topology) {
        return new CorrelationEngine(scorer(props, topology));
    }

    public static CorrelationEngine engine() {
        return engine(defaultProperties(), TestTopology.standard());
    }
}
