package com.pulseops.correlation.score;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.support.CorrelationFixture;
import com.pulseops.support.Events;
import org.junit.jupiter.api.Test;

import static com.pulseops.events.model.EventType.HTTP_500;
import static com.pulseops.events.model.Severity.HIGH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TemporalScorerTest {

    private final TemporalScorer scorer = new TemporalScorer(CorrelationFixture.defaultProperties());

    @Test
    void identicalTimestampsScoreOne() {
        SignalEvent a = Events.event(1, "payment-api", HTTP_500, HIGH, Events.plusSeconds(0));
        SignalEvent b = Events.event(2, "payment-api", HTTP_500, HIGH, Events.plusSeconds(0));
        assertThat(scorer.rawScore(a, b)).isEqualTo(1.0);
    }

    @Test
    void scoreIsHalfAtOneHalfLife() {
        SignalEvent a = Events.event(1, "payment-api", HTTP_500, HIGH, Events.plusSeconds(0));
        SignalEvent b = Events.event(2, "payment-api", HTTP_500, HIGH, Events.plusSeconds(120));
        assertThat(scorer.rawScore(a, b)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void scoreDecaysMonotonicallyWithGap() {
        SignalEvent a = Events.event(1, "payment-api", HTTP_500, HIGH, Events.plusSeconds(0));
        double near = scorer.rawScore(a, Events.event(2, "payment-api", HTTP_500, HIGH, Events.plusSeconds(30)));
        double far = scorer.rawScore(a, Events.event(3, "payment-api", HTTP_500, HIGH, Events.plusSeconds(300)));
        assertThat(near).isGreaterThan(far);
    }
}
