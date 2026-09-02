package com.pulseops.incident;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.model.Severity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Derives an incident's severity from its events. Rules, in order:
 * <ol>
 *   <li>start from the highest single event severity;</li>
 *   <li>escalate one level if the incident spans 3+ services (blast radius);</li>
 *   <li>escalate one level if there are 2+ CRITICAL events;</li>
 *   <li>cap at CRITICAL.</li>
 * </ol>
 * Deterministic and easy to explain in a review.
 */
@Component
public class SeverityCalculator {

    public Severity calculate(List<SignalEvent> events) {
        Severity base = events.stream()
                .map(SignalEvent::severity)
                .reduce(Severity.LOW, Severity::max);

        int distinctServices = (int) events.stream().map(SignalEvent::service).distinct().count();
        long criticalCount = events.stream().filter(e -> e.severity() == Severity.CRITICAL).count();

        int level = base.ordinal();
        if (distinctServices >= 3) {
            level++;
        }
        if (criticalCount >= 2) {
            level++;
        }
        level = Math.min(level, Severity.CRITICAL.ordinal());
        return Severity.values()[level];
    }
}
