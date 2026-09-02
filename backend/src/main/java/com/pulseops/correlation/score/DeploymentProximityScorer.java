package com.pulseops.correlation.score;

import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.model.EventType;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Whether a deployment plausibly links the two events. Strong signal when:
 * <ul>
 *   <li>both events reference the same deployment id, or</li>
 *   <li>one event is a {@code DEPLOYMENT} and the other occurred shortly after
 *       it (within the configured lookback).</li>
 * </ul>
 * A deployment is one of the few things that is both a discrete change and a
 * common root cause, so it earns its own dimension.
 */
@Component
public class DeploymentProximityScorer implements PairScorer {

    private final double weight;
    private final long lookbackSeconds;

    public DeploymentProximityScorer(CorrelationProperties props) {
        this.weight = props.weights().normalized().deployment();
        this.lookbackSeconds = props.deployment().lookbackMinutes() * 60;
    }

    @Override
    public String signal() {
        return "deployment";
    }

    @Override
    public double weight() {
        return weight;
    }

    @Override
    public double rawScore(SignalEvent a, SignalEvent b) {
        if (a.deploymentId() != null && a.deploymentId().equals(b.deploymentId())) {
            return 1.0;
        }
        Double followScore = deploymentFollowedBy(a, b);
        if (followScore != null) {
            return followScore;
        }
        followScore = deploymentFollowedBy(b, a);
        return followScore != null ? followScore : 0.0;
    }

    /** If {@code maybeDeploy} is a DEPLOYMENT and {@code other} follows it within lookback. */
    private Double deploymentFollowedBy(SignalEvent maybeDeploy, SignalEvent other) {
        if (maybeDeploy.eventType() != EventType.DEPLOYMENT) {
            return null;
        }
        long deltaSeconds = Duration.between(maybeDeploy.occurredAt(), other.occurredAt()).toSeconds();
        if (deltaSeconds < 0 || deltaSeconds > lookbackSeconds) {
            return null;
        }
        // Linear decay across the lookback window: right after deploy -> ~1.0.
        return 0.9 * (1.0 - (double) deltaSeconds / lookbackSeconds) + 0.1;
    }

    @Override
    public String explain(SignalEvent a, SignalEvent b) {
        if (a.deploymentId() != null && a.deploymentId().equals(b.deploymentId())) {
            return "both events reference deployment DEP-" + a.deploymentId();
        }
        SignalEvent deploy = a.eventType() == EventType.DEPLOYMENT ? a
                : b.eventType() == EventType.DEPLOYMENT ? b : null;
        if (deploy != null) {
            return "follows a deployment to " + deploy.service();
        }
        return "no deployment link";
    }
}
