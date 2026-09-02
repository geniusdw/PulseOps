package com.pulseops.simulator;

import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds the ordered event script for each scenario. Scripts are hand-authored
 * to look like a real cascading failure: an originating signal, then dependent
 * services degrading, then user-facing errors. Small random jitter is added to
 * values so repeated runs are not identical.
 */
@Component
public class ScenarioGenerator {

    public List<PlannedEvent> plan(Scenario scenario) {
        return switch (scenario) {
            case DATABASE_FAILURE -> databaseFailure();
            case BAD_DEPLOYMENT -> badDeployment();
            case CPU_SATURATION -> cpuSaturation();
            case NETWORK_FAILURE -> networkFailure();
            case NORMAL_TRAFFIC -> normalTraffic();
        };
    }

    private List<PlannedEvent> databaseFailure() {
        return List.of(
                PlannedEvent.of(0, "database", EventType.DB_CONNECTION_EXHAUSTION, Severity.HIGH,
                        "db_connections_used", jitter(100, 0), "Connection pool exhausted (100/100 in use)"),
                PlannedEvent.of(9, "transaction-service", EventType.HIGH_LATENCY, Severity.HIGH,
                        "latency_p99_ms", jitter(4200, 300), "p99 latency rose to 4.2s waiting on DB"),
                PlannedEvent.of(17, "payment-api", EventType.HIGH_LATENCY, Severity.HIGH,
                        "latency_p99_ms", jitter(5100, 400), "Upstream latency from transaction-service"),
                PlannedEvent.of(24, "payment-api", EventType.HTTP_500, Severity.CRITICAL,
                        "error_rate", jitter(22, 4), "HTTP 500 rate 22% — DB query timeouts"),
                PlannedEvent.of(31, "transaction-service", EventType.HTTP_500, Severity.HIGH,
                        "error_rate", jitter(14, 3), "HTTP 500 rate 14%"),
                PlannedEvent.of(38, "payment-api", EventType.PAYMENT_FAILURE, Severity.CRITICAL,
                        "failed_payments", jitter(37, 6), "37 payment attempts failed in the last minute")
        );
    }

    private List<PlannedEvent> badDeployment() {
        List<PlannedEvent> events = new ArrayList<>();
        events.add(PlannedEvent.deployment(0, "payment-api", "v2.4.0",
                "Deployment payment-api v2.4.0 rolled out"));
        events.add(PlannedEvent.linked(42, "payment-api", EventType.HIGH_LATENCY, Severity.HIGH,
                "latency_p99_ms", jitter(3300, 250), "p99 latency doubled after rollout"));
        events.add(PlannedEvent.linked(58, "payment-api", EventType.HTTP_500, Severity.HIGH,
                "error_rate", jitter(9, 2), "HTTP 500 rate 9% post-deploy"));
        events.add(PlannedEvent.linked(74, "payment-api", EventType.SERVICE_RESTART, Severity.MEDIUM,
                null, null, "payment-api pod restarted (OOM after deploy)"));
        events.add(PlannedEvent.linked(96, "payment-api", EventType.HTTP_500, Severity.CRITICAL,
                "error_rate", jitter(28, 5), "HTTP 500 rate 28% — regression confirmed"));
        events.add(PlannedEvent.linked(112, "api-gateway", EventType.HTTP_503, Severity.HIGH,
                "error_rate", jitter(12, 3), "Gateway shedding load to payment-api"));
        return events;
    }

    private List<PlannedEvent> cpuSaturation() {
        return List.of(
                PlannedEvent.of(0, "user-service", EventType.CPU_SPIKE, Severity.HIGH,
                        "cpu_percent", jitter(97, 2), "CPU pinned at 97% for 60s"),
                PlannedEvent.of(11, "user-service", EventType.HIGH_LATENCY, Severity.HIGH,
                        "latency_p99_ms", jitter(2600, 200), "Request queueing under CPU pressure"),
                PlannedEvent.of(21, "user-service", EventType.HTTP_503, Severity.HIGH,
                        "error_rate", jitter(18, 4), "HTTP 503 — thread pool saturated"),
                PlannedEvent.of(30, "api-gateway", EventType.HTTP_503, Severity.MEDIUM,
                        "error_rate", jitter(7, 2), "Gateway 503s for /users routes")
        );
    }

    private List<PlannedEvent> networkFailure() {
        return List.of(
                PlannedEvent.of(0, "api-gateway", EventType.NETWORK_ERROR, Severity.HIGH,
                        "connect_timeouts", jitter(45, 8), "Upstream connect timeouts to notification-service"),
                PlannedEvent.of(9, "notification-service", EventType.HTTP_503, Severity.HIGH,
                        "error_rate", jitter(30, 6), "Service unreachable — 503"),
                PlannedEvent.of(17, "api-gateway", EventType.HTTP_503, Severity.HIGH,
                        "error_rate", jitter(15, 3), "Gateway 503s on notification routes"),
                PlannedEvent.of(26, "notification-service", EventType.SERVICE_RESTART, Severity.MEDIUM,
                        null, null, "notification-service restarted to recover connections")
        );
    }

    /**
     * Background noise: low severity, spread across services and ~4 minutes so no
     * pair clears the correlation threshold. Proves the engine does not cry wolf.
     */
    private List<PlannedEvent> normalTraffic() {
        String[] services = {"api-gateway", "payment-api", "user-service", "notification-service", "transaction-service"};
        EventType[] types = {EventType.HIGH_LATENCY, EventType.CPU_SPIKE, EventType.QUEUE_BACKLOG};
        List<PlannedEvent> events = new ArrayList<>();
        long offset = 0;
        for (int i = 0; i < 12; i++) {
            String service = services[i % services.length];
            EventType type = types[i % types.length];
            events.add(PlannedEvent.of(offset, service, type, Severity.LOW,
                    "value", jitter(30, 15), "Nominal " + type + " sample"));
            offset += ThreadLocalRandom.current().nextInt(9, 15);
        }
        return events;
    }

    private static double jitter(double base, double spread) {
        if (spread <= 0) {
            return base;
        }
        return Math.round((base + ThreadLocalRandom.current().nextDouble(-spread, spread)) * 10.0) / 10.0;
    }
}
