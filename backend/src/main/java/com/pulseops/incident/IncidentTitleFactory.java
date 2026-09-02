package com.pulseops.incident;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Generates a readable incident title from its events. */
@Component
public class IncidentTitleFactory {

    public String titleFor(List<SignalEvent> events, Severity severity) {
        String dominantService = mode(events.stream().map(SignalEvent::service).toList());
        EventType dominantType = events.stream()
                .filter(e -> e.eventType() != EventType.DEPLOYMENT)
                .map(SignalEvent::eventType)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(events.get(0).eventType());

        long serviceCount = events.stream().map(SignalEvent::service).distinct().count();
        String suffix = serviceCount > 1 ? " affecting " + serviceCount + " services" : "";
        return "%s: %s on %s%s".formatted(
                severity.name(), humanType(dominantType), dominantService, suffix);
    }

    private static String humanType(EventType type) {
        return switch (type) {
            case HTTP_500 -> "HTTP 500 errors";
            case HTTP_503 -> "HTTP 503 errors";
            case HIGH_LATENCY -> "elevated latency";
            case CPU_SPIKE -> "CPU saturation";
            case MEMORY_SPIKE -> "memory saturation";
            case DB_CONNECTION_EXHAUSTION -> "database connection exhaustion";
            case NETWORK_ERROR -> "network errors";
            case SERVICE_RESTART -> "service restarts";
            case PAYMENT_FAILURE -> "payment failures";
            case QUEUE_BACKLOG -> "queue backlog";
            case DEPLOYMENT -> "deployment";
        };
    }

    private static <T> T mode(List<T> values) {
        return values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(values.get(0));
    }
}
