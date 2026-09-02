package com.pulseops.correlation;

import com.pulseops.correlation.model.PairCorrelation;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.correlation.score.PairScorer;
import com.pulseops.correlation.score.SubScore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aggregates every {@link PairScorer} into one weighted correlation score for a
 * pair of events. Because weights are normalised to sum to 1, the result is the
 * weighted average of the sub-scores and stays in {@code [0,1]}.
 */
@Component
public class CorrelationScorer {

    private final List<PairScorer> scorers;
    private final double threshold;

    public CorrelationScorer(List<PairScorer> scorers, CorrelationProperties props) {
        this.scorers = List.copyOf(scorers);
        this.threshold = props.threshold();
    }

    public PairCorrelation evaluate(SignalEvent a, SignalEvent b) {
        List<SubScore> subScores = scorers.stream()
                .map(scorer -> scorer.score(a, b))
                .toList();
        double total = subScores.stream().mapToDouble(SubScore::weightedValue).sum();
        return new PairCorrelation(a, b, total, total >= threshold, subScores);
    }

    public double threshold() {
        return threshold;
    }
}
