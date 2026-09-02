package com.pulseops.ingest;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.incident.IncidentReconciliationCoordinator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The consumer side of the producer/consumer pipeline: a fixed pool of worker
 * threads that pull batches off {@link IngestQueue} and drive incident
 * reconciliation.
 *
 * <p>Design:
 * <ul>
 *   <li><b>Fixed thread pool</b> ({@code workerCount}) — correlation is
 *       CPU-bound, so more threads than cores would only add context switching.
 *       The count is configurable so the benchmark can sweep it.</li>
 *   <li><b>Batch drain</b> — each worker takes up to {@code batchSize} events at
 *       once; correlation then runs once per batch, not once per event.</li>
 *   <li><b>Graceful shutdown</b> — {@code @PreDestroy} flips the running flag,
 *       stops the pool and waits, so in-flight batches finish.</li>
 *   <li><b>Failure isolation</b> — an exception in one batch is logged and
 *       counted; the worker loops on to the next batch.</li>
 * </ul>
 */
@Component
public class EventProcessingPipeline {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingPipeline.class);

    private final IngestQueue queue;
    private final IncidentReconciliationCoordinator coordinator;
    private final PipelineMetrics metrics;
    private final int workerCount;
    private final int batchSize;
    private final long pollTimeoutMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService pool;

    public EventProcessingPipeline(IngestQueue queue,
                                   IncidentReconciliationCoordinator coordinator,
                                   PipelineMetrics metrics,
                                   IngestProperties props) {
        this.queue = queue;
        this.coordinator = coordinator;
        this.metrics = metrics;
        this.workerCount = props.workerCount();
        this.batchSize = props.batchSize();
        this.pollTimeoutMs = props.pollTimeoutMs();
    }

    @PostConstruct
    public void start() {
        running.set(true);
        pool = Executors.newFixedThreadPool(workerCount, namedThreadFactory());
        for (int i = 0; i < workerCount; i++) {
            pool.submit(this::workerLoop);
        }
        log.info("Event processing pipeline started with {} workers, batch size {}", workerCount, batchSize);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping event processing pipeline");
        running.set(false);
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                List<SignalEvent> batch = queue.drainBatch(batchSize, pollTimeoutMs);
                if (batch.isEmpty()) {
                    continue;
                }
                coordinator.onBatchProcessed(batch);
                metrics.recordProcessed(batch.size());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                metrics.recordFailed(1);
                log.error("Worker failed processing a batch", ex);
            }
        }
    }

    private static ThreadFactory namedThreadFactory() {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread t = new Thread(runnable, "pulseops-worker-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
