package com.pulseops.incident.api;

import java.util.List;

/**
 * Ranked root-cause hypotheses for an incident.
 *
 * <p>{@code disclaimer} is part of the payload on purpose: these are heuristic
 * scores from rules over the correlated events, not calibrated probabilities.
 */
public record RootCauseResponse(
        String incidentId,
        List<RootCauseDto> candidates,
        String disclaimer
) {
    public static final String DISCLAIMER =
            "Heuristic root-cause scores derived from rules over the correlated events. "
            + "They rank hypotheses; they are not statistical probabilities.";
}
