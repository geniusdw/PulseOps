package com.pulseops.ingest;

import com.pulseops.common.error.CapacityExceededException;
import com.pulseops.correlation.model.SignalEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The single hand-off point between producers (ingest controller, simulator) and
 * the worker pool that runs correlation.
 *
 * <p>Why a bounded {@link BlockingQueue}:
 * <ul>
 *   <li><b>Decoupling</b> — producers return as soon as the event is queued;
 *       they don't wait for correlation.</li>
 *   <li><b>Backpressure</b> — the queue is bounded. When it fills,
 *       {@link #submit} fails fast with a 429 instead of the JVM slowly running
 *       out of heap. An unbounded queue would just hide the overload.</li>
 *   <li><b>Batching</b> — workers {@link #drainBatch drain} many events at once,
 *       so correlation runs over a window rather than per-event.</li>
 * </ul>
 * {@code ArrayBlockingQueue} (array-backed, fixed capacity) is the right choice
 * here: capacity is known at startup and it has lower per-item overhead than a
 * linked queue.
 */
@Component
public class IngestQueue {

    private final BlockingQueue<SignalEvent> queue;
    private final long offerTimeoutMs;
    private final PipelineMetrics metrics;

    public IngestQueue(IngestProperties props, PipelineMetrics metrics) {
        this.queue = new ArrayBlockingQueue<>(props.queueCapacity());
        this.offerTimeoutMs = props.offerTimeoutMs();
        this.metrics = metrics;
    }

    /**
     * Enqueue an event for correlation, waiting up to the configured offer
     * timeout if the queue is full.
     *
     * @throws CapacityExceededException if the event could not be enqueued in time
     */
    public void submit(SignalEvent event) {
        try {
            boolean accepted = queue.offer(event, offerTimeoutMs, TimeUnit.MILLISECONDS);
            if (!accepted) {
                metrics.recordRejected();
                throw new CapacityExceededException(
                        "Ingest queue is full (capacity reached); retry after backoff");
            }
            metrics.recordEnqueued();
            metrics.observeQueueDepth(queue.size());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CapacityExceededException("Interrupted while enqueuing event");
        }
    }

    /**
     * Block for up to {@code pollTimeoutMs} for the first event, then drain up to
     * {@code maxBatch - 1} more that are already waiting. Returns an empty list if
     * nothing arrived within the timeout (so the worker can re-check its running
     * flag).
     */
    public List<SignalEvent> drainBatch(int maxBatch, long pollTimeoutMs) throws InterruptedException {
        List<SignalEvent> batch = new ArrayList<>(maxBatch);
        SignalEvent first = queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
        if (first == null) {
            return batch;
        }
        batch.add(first);
        queue.drainTo(batch, maxBatch - 1);
        return batch;
    }

    public int size() {
        return queue.size();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }
}
