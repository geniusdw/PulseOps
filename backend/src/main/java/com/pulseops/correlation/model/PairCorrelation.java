package com.pulseops.correlation.model;

import com.pulseops.correlation.score.SubScore;

import java.util.List;

/**
 * The full, explainable result of scoring one pair of events.
 *
 * @param a         first event
 * @param b         second event
 * @param score     aggregate weighted score in {@code [0,1]}
 * @param linked    true when {@code score >= configured threshold}
 * @param subScores per-signal breakdown, for explanations
 */
public record PairCorrelation(
        SignalEvent a,
        SignalEvent b,
        double score,
        boolean linked,
        List<SubScore> subScores
) {
}
