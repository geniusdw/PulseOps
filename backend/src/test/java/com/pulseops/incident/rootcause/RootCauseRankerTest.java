package com.pulseops.incident.rootcause;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.support.Events;
import com.pulseops.support.TestTopology;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pulseops.events.model.EventType.DB_CONNECTION_EXHAUSTION;
import static com.pulseops.events.model.EventType.HIGH_LATENCY;
import static com.pulseops.events.model.EventType.HTTP_500;
import static com.pulseops.events.model.EventType.PAYMENT_FAILURE;
import static com.pulseops.events.model.EventType.SERVICE_RESTART;
import static com.pulseops.events.model.Severity.CRITICAL;
import static com.pulseops.events.model.Severity.HIGH;
import static com.pulseops.events.model.Severity.MEDIUM;
import static org.assertj.core.api.Assertions.assertThat;

class RootCauseRankerTest {

    private final RootCauseRanker ranker = new RootCauseRanker(TestTopology.standard());

    @Test
    void databaseFailureScenarioRanksDatabaseFirst() {
        List<SignalEvent> events = List.of(
                Events.event(1, "database", DB_CONNECTION_EXHAUSTION, HIGH, Events.plusSeconds(0)),
                Events.event(2, "transaction-service", HIGH_LATENCY, HIGH, Events.plusSeconds(9)),
                Events.event(3, "payment-api", HTTP_500, CRITICAL, Events.plusSeconds(20)),
                Events.event(4, "payment-api", PAYMENT_FAILURE, CRITICAL, Events.plusSeconds(28)));

        List<RootCauseCandidate> ranked = ranker.rank(events, List.of());

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).type()).isEqualTo(RootCauseType.DATABASE_FAILURE);
        assertThat(ranked.get(0).score()).isBetween(0.0, 1.0);
        assertThat(ranked.get(0).evidence()).isNotEmpty();
    }

    @Test
    void badDeploymentScenarioRanksDeploymentFirst() {
        DeploymentEntity deployment = new DeploymentEntity("payment-api", "v2.4.0", Events.plusSeconds(0));
        List<SignalEvent> events = List.of(
                Events.event(2, "payment-api", HIGH_LATENCY, HIGH, Events.plusSeconds(42)),
                Events.event(3, "payment-api", HTTP_500, HIGH, Events.plusSeconds(58)),
                Events.event(4, "payment-api", SERVICE_RESTART, MEDIUM, Events.plusSeconds(74)),
                Events.event(5, "payment-api", HTTP_500, CRITICAL, Events.plusSeconds(96)));

        List<RootCauseCandidate> ranked = ranker.rank(events, List.of(deployment));

        assertThat(ranked.get(0).type()).isEqualTo(RootCauseType.DEPLOYMENT);
    }

    @Test
    void scoresAreLabelledHeuristicNotProbabilistic_andSumMayExceedOne() {
        List<SignalEvent> events = List.of(
                Events.event(1, "database", DB_CONNECTION_EXHAUSTION, HIGH, Events.plusSeconds(0)),
                Events.event(2, "payment-api", HTTP_500, CRITICAL, Events.plusSeconds(10)));

        double total = ranker.rank(events, List.of()).stream()
                .mapToDouble(RootCauseCandidate::score).sum();

        // Independent hypotheses — they are NOT a probability distribution.
        assertThat(total).isGreaterThan(0.0);
    }
}
