package com.pulseops.incident.rootcause;

import java.util.List;

/**
 * One ranked root-cause hypothesis.
 *
 * @param type     category
 * @param label    display label (may include a specific service / deployment)
 * @param score    heuristic confidence in {@code [0,1]} — NOT a probability
 * @param evidence the rules that fired, as human-readable strings
 */
public record RootCauseCandidate(
        RootCauseType type,
        String label,
        double score,
        List<String> evidence
) {
}
