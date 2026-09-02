package com.pulseops.events;

import com.pulseops.common.error.ValidationException;
import com.pulseops.events.dto.EventRequest;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import com.pulseops.topology.TopologyService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Semantic validation + normalisation of an incoming event. Bean Validation on
 * the DTO has already checked "required / max length"; this checks meaning:
 * known service, known event type / severity, sane timestamp.
 *
 * <p>Collects <em>all</em> problems and throws once, so a caller fixing a bad
 * payload sees every issue at once rather than one per round-trip.
 */
@Component
public class EventValidator {

    /** Reject timestamps more than this far in the future (clock skew tolerance). */
    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(2);
    /** Reject absurdly old timestamps (likely a unit/format bug). */
    private static final Duration MAX_AGE = Duration.ofDays(30);

    private final TopologyService topology;
    private final Clock clock;

    public EventValidator(TopologyService topology, Clock clock) {
        this.topology = topology;
        this.clock = clock;
    }

    public NormalizedEvent validate(EventRequest request) {
        List<String> problems = new ArrayList<>();
        Instant now = clock.instant();

        Instant occurredAt = request.timestamp() != null ? request.timestamp() : now;
        if (occurredAt.isAfter(now.plus(MAX_FUTURE_SKEW))) {
            problems.add("timestamp is in the future: " + occurredAt);
        }
        if (occurredAt.isBefore(now.minus(MAX_AGE))) {
            problems.add("timestamp is more than 30 days old: " + occurredAt);
        }

        String service = trimToNull(request.service());
        if (service != null && !topology.exists(service)) {
            problems.add("unknown service '" + service + "'");
        }

        EventType eventType = parseEnum(EventType.class, request.eventType(), "eventType", problems);
        Severity severity = parseEnum(Severity.class, request.severity(), "severity", problems);

        String message = trimToNull(request.message());

        if (!problems.isEmpty()) {
            throw new ValidationException("Event failed validation", problems);
        }

        return new NormalizedEvent(
                occurredAt,
                service,
                trimToNull(request.host()),
                eventType,
                severity,
                trimToNull(request.metric()),
                request.value(),
                message,
                trimToNull(request.deploymentVersion())
        );
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field, List<String> problems) {
        if (raw == null || raw.isBlank()) {
            return null; // already reported by bean validation
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            problems.add("unknown " + field + " '" + raw + "'");
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
