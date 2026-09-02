package com.pulseops.ingest;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe counters for the ingest pipeline, shared by the producers
 * (controllers, simulator), the queue and the worker pool.
 *
 * <p>All fields are {@code Atomic*} because they are written from many threads
 * with no lock. They are monotonic counters and a high-water mark, so atomics
 * are sufficient — no compound invariant needs a lock.
 */
@Component
public class PipelineMetrics {

    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicInteger peakQueueDepth = new AtomicInteger();

    public void recordEnqueued() {
        enqueued.incrementAndGet();
    }

    public void recordRejected() {
        rejected.incrementAndGet();
    }

    public void recordProcessed(long count) {
        processed.addAndGet(count);
    }

    public void recordFailed(long count) {
        failed.addAndGet(count);
    }

    /** Update the high-water mark for queue depth (called on every enqueue). */
    public void observeQueueDepth(int depth) {
        peakQueueDepth.accumulateAndGet(depth, Math::max);
    }

    public long enqueued() {
        return enqueued.get();
    }

    public long rejected() {
        return rejected.get();
    }

    public long processed() {
        return processed.get();
    }

    public long failed() {
        return failed.get();
    }

    public int peakQueueDepth() {
        return peakQueueDepth.get();
    }

    public void resetPeak() {
        peakQueueDepth.set(0);
    }
}
