package com.pulseops.correlation.score;

/**
 * One dimension of a pairwise correlation score.
 *
 * @param signal    which signal this is (temporal, service-dependency, ...)
 * @param rawValue  the signal's strength in {@code [0,1]} before weighting
 * @param weight    the configured weight applied to this signal (already normalised)
 * @param detail    a short human-readable justification, e.g. "42s apart"
 */
public record SubScore(String signal, double rawValue, double weight, String detail) {

    public double weightedValue() {
        return rawValue * weight;
    }
}
