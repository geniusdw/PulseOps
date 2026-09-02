package com.pulseops.common.error;

/**
 * Thrown when the bounded ingest queue is full and a producer could not enqueue
 * an event within the configured offer timeout. Mapped to HTTP 429 so callers
 * (and the simulator) can back off. This is backpressure made visible.
 */
public class CapacityExceededException extends RuntimeException {

    public CapacityExceededException(String message) {
        super(message);
    }
}
