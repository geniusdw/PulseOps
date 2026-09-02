package com.pulseops.correlation.model;

import java.util.List;

/**
 * How much one signal (temporal, service-dependency, ...) contributed to a
 * cluster's grouping, averaged over the cluster's linking pairs.
 */
public record SignalContribution(
        String signal,
        double averageRawValue,
        double averageWeightedContribution,
        List<String> sampleDetails
) {
}
