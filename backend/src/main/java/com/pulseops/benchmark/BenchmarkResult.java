package com.pulseops.benchmark;

public record BenchmarkResult(
        int eventCount,
        int workerCount,
        int windowSize,
        int windowsProcessed,
        int clustersFound,
        long elapsedMs,
        double throughputPerSecond,
        String note
) {
    public static final String NOTE =
            "Measured on this machine, now. Correlation is CPU-bound and O(n^2) within a window, "
            + "so throughput depends on core count, window size and JIT warm-up. "
            + "These numbers are a local measurement, not a benchmark claim.";
}
