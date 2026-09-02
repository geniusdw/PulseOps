package com.pulseops.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding of {@code pulseops.ingest.*}. Every value is a tuning knob for
 * the producer/consumer pipeline.
 *
 * @param queueCapacity  size of the bounded {@link java.util.concurrent.BlockingQueue};
 *                       when full, producers are rejected (backpressure)
 * @param workerCount    number of consumer threads in the pool
 * @param batchSize      max events a worker drains from the queue per cycle
 * @param pollTimeoutMs  how long a worker blocks waiting for the next event
 * @param offerTimeoutMs how long a producer blocks trying to enqueue before 429
 */
@ConfigurationProperties(prefix = "pulseops.ingest")
public record IngestProperties(
        int queueCapacity,
        int workerCount,
        int batchSize,
        long pollTimeoutMs,
        long offerTimeoutMs
) {
    public IngestProperties {
        if (queueCapacity < 1) throw new IllegalArgumentException("queueCapacity must be >= 1");
        if (workerCount < 1) throw new IllegalArgumentException("workerCount must be >= 1");
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be >= 1");
    }
}
