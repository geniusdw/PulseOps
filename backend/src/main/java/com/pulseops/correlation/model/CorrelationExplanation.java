package com.pulseops.correlation.model;

import java.util.List;

/**
 * Why a set of events was grouped. Both a one-line {@code summary} for humans and
 * the structured breakdown behind it, so the dashboard can render either.
 */
public record CorrelationExplanation(
        String summary,
        int eventCount,
        int linkCount,
        long timeSpanSeconds,
        List<String> services,
        List<String> deploymentIds,
        List<SignalContribution> topSignals
) {
}
