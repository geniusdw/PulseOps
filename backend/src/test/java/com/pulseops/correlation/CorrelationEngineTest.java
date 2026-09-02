package com.pulseops.correlation;

import com.pulseops.correlation.model.EventCluster;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.support.CorrelationFixture;
import com.pulseops.support.Events;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.pulseops.events.model.EventType.CPU_SPIKE;
import static com.pulseops.events.model.EventType.DB_CONNECTION_EXHAUSTION;
import static com.pulseops.events.model.EventType.HIGH_LATENCY;
import static com.pulseops.events.model.EventType.HTTP_500;
import static com.pulseops.events.model.EventType.PAYMENT_FAILURE;
import static com.pulseops.events.model.Severity.CRITICAL;
import static com.pulseops.events.model.Severity.HIGH;
import static com.pulseops.events.model.Severity.LOW;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationEngineTest {

    private final CorrelationEngine engine = CorrelationFixture.engine();

    @Test
    void groupsATemporalDependencyChainIntoOneCluster() {
        List<SignalEvent> events = List.of(
                Events.event(1, "database", DB_CONNECTION_EXHAUSTION, HIGH, Events.plusSeconds(0)),
                Events.event(2, "transaction-service", HIGH_LATENCY, HIGH, Events.plusSeconds(9)),
                Events.event(3, "payment-api", HTTP_500, CRITICAL, Events.plusSeconds(20)),
                Events.event(4, "payment-api", PAYMENT_FAILURE, CRITICAL, Events.plusSeconds(28)));

        List<EventCluster> clusters = engine.correlate(events);

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).events()).hasSize(4);
        assertThat(clusters.get(0).explanation().services())
                .contains("database", "payment-api", "transaction-service");
    }

    @Test
    void doesNotGroupEventsThatAreFarApartInTimeAndUnrelated() {
        List<SignalEvent> events = List.of(
                Events.event(1, "payment-api", HTTP_500, LOW, Events.plusSeconds(0)),
                Events.event(2, "user-service", CPU_SPIKE, LOW, Events.plusSeconds(9 * 60)));

        assertThat(engine.correlate(events)).isEmpty();
    }

    @Test
    void doesNotGroupUnrelatedServicesWithWeakSignals() {
        // Same instant, but unrelated services, low severity, unrelated types.
        Instant now = Events.plusSeconds(0);
        List<SignalEvent> events = List.of(
                Events.event(1, "notification-service", CPU_SPIKE, LOW, now),
                Events.event(2, "database", PAYMENT_FAILURE, LOW, now));

        // notification-service and database have no dependency path; combined score
        // should stay below threshold despite temporal coincidence.
        assertThat(engine.correlate(events)).isEmpty();
    }

    @Test
    void clusterStrengthIsBetweenZeroAndOne() {
        List<SignalEvent> events = List.of(
                Events.event(1, "database", DB_CONNECTION_EXHAUSTION, HIGH, Events.plusSeconds(0)),
                Events.event(2, "transaction-service", HIGH_LATENCY, HIGH, Events.plusSeconds(5)));

        List<EventCluster> clusters = engine.correlate(events);
        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).strength()).isBetween(0.0, 1.0);
    }

    @Test
    void singleEventProducesNoCluster() {
        assertThat(engine.correlate(List.of(
                Events.event(1, "payment-api", HTTP_500, HIGH, Events.plusSeconds(0))))).isEmpty();
    }
}
