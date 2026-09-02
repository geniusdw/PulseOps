package com.pulseops.correlation.model;

import java.time.Instant;
import java.util.List;

/**
 * A group of events the engine believes belong to the same incident, with the
 * evidence: the linking pairs and a human explanation.
 *
 * @param events      cluster members, oldest first
 * @param links       the pairwise correlations that connected them (score >= threshold)
 * @param strength    average link score in {@code [0,1]} — the confidence input
 * @param explanation why they were grouped
 */
public record EventCluster(
        List<SignalEvent> events,
        List<PairCorrelation> links,
        double strength,
        CorrelationExplanation explanation
) {
    public Instant startedAt() {
        return events.get(0).occurredAt();
    }

    public Instant latestAt() {
        return events.get(events.size() - 1).occurredAt();
    }

    public List<Long> eventIds() {
        return events.stream().map(SignalEvent::id).toList();
    }
}
