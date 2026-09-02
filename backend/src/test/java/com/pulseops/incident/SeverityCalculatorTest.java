package com.pulseops.incident;

import com.pulseops.support.Events;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pulseops.events.model.EventType.HTTP_500;
import static com.pulseops.events.model.EventType.HTTP_503;
import static com.pulseops.events.model.EventType.PAYMENT_FAILURE;
import static com.pulseops.events.model.Severity.CRITICAL;
import static com.pulseops.events.model.Severity.HIGH;
import static com.pulseops.events.model.Severity.LOW;
import static com.pulseops.events.model.Severity.MEDIUM;
import static org.assertj.core.api.Assertions.assertThat;

class SeverityCalculatorTest {

    private final SeverityCalculator calculator = new SeverityCalculator();

    @Test
    void startsFromHighestSingleEventSeverity() {
        assertThat(calculator.calculate(List.of(
                Events.event("payment-api", HTTP_500, LOW, Events.plusSeconds(0)),
                Events.event("payment-api", HTTP_503, MEDIUM, Events.plusSeconds(5)))))
                .isEqualTo(MEDIUM);
    }

    @Test
    void escalatesWhenBlastRadiusIsThreeOrMoreServices() {
        assertThat(calculator.calculate(List.of(
                Events.event("payment-api", HTTP_500, MEDIUM, Events.plusSeconds(0)),
                Events.event("user-service", HTTP_503, MEDIUM, Events.plusSeconds(5)),
                Events.event("api-gateway", HTTP_503, MEDIUM, Events.plusSeconds(9)))))
                .isEqualTo(HIGH);
    }

    @Test
    void escalatesWhenMultipleCriticalEvents() {
        assertThat(calculator.calculate(List.of(
                Events.event("payment-api", HTTP_500, HIGH, Events.plusSeconds(0)),
                Events.event("payment-api", PAYMENT_FAILURE, CRITICAL, Events.plusSeconds(5)),
                Events.event("payment-api", HTTP_500, CRITICAL, Events.plusSeconds(9)))))
                .isEqualTo(CRITICAL);
    }

    @Test
    void neverExceedsCritical() {
        assertThat(calculator.calculate(List.of(
                Events.event("payment-api", HTTP_500, CRITICAL, Events.plusSeconds(0)),
                Events.event("user-service", HTTP_503, CRITICAL, Events.plusSeconds(3)),
                Events.event("api-gateway", HTTP_503, CRITICAL, Events.plusSeconds(6)))))
                .isEqualTo(CRITICAL);
    }
}
