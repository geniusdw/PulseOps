package com.pulseops.benchmark;

import com.pulseops.correlation.CorrelationEngine;
import com.pulseops.correlation.model.EventCluster;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import com.pulseops.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Measures correlation throughput on this machine, right now.
 *
 * <p>It generates synthetic events in memory, partitions them into windows, and
 * runs {@link CorrelationEngine#correlate} across a configurable-size thread
 * pool — the CPU-bound core of PulseOps, isolated from the database. It never
 * writes to MySQL, so it is safe to run repeatedly.
 *
 * <p>Nothing here is hard-coded or cached: every call does the work and times it.
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private static final int MAX_EVENTS = 200_000;
    private static final int MAX_WORKERS = 64;

    private final CorrelationEngine engine;
    private final TopologyService topology;

    public BenchmarkService(CorrelationEngine engine, TopologyService topology) {
        this.engine = engine;
        this.topology = topology;
    }

    public BenchmarkResult run(int requestedCount, int requestedWorkers, int requestedWindowSize) {
        int count = clamp(requestedCount, 1, MAX_EVENTS);
        int workers = clamp(requestedWorkers, 1, MAX_WORKERS);
        int windowSize = clamp(requestedWindowSize, 10, 5_000);

        List<String> services = new ArrayList<>(topology.allServiceNames());
        List<List<SignalEvent>> windows = buildWindows(count, windowSize, services);

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        long startNanos = System.nanoTime();
        int clusters = 0;
        try {
            List<Future<List<EventCluster>>> futures = new ArrayList<>(windows.size());
            for (List<SignalEvent> window : windows) {
                futures.add(pool.submit(() -> engine.correlate(window)));
            }
            for (Future<List<EventCluster>> future : futures) {
                clusters += future.get().size();
            }
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Benchmark run failed", ex);
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        double throughput = elapsedMs == 0 ? count : count / (elapsedMs / 1000.0);

        log.info("Benchmark: {} events, {} workers, window {} -> {} ms ({} evt/s)",
                count, workers, windowSize, elapsedMs, Math.round(throughput));

        return new BenchmarkResult(count, workers, windowSize, windows.size(), clusters,
                elapsedMs, Math.round(throughput * 10.0) / 10.0, BenchmarkResult.NOTE);
    }

    private List<List<SignalEvent>> buildWindows(int count, int windowSize, List<String> services) {
        EventType[] types = EventType.values();
        Severity[] severities = Severity.values();
        Instant base = Instant.now().minusSeconds(count);

        List<List<SignalEvent>> windows = new ArrayList<>();
        List<SignalEvent> current = new ArrayList<>(windowSize);
        for (int i = 0; i < count; i++) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            SignalEvent event = new SignalEvent(
                    i + 1L,
                    services.get(rnd.nextInt(services.size())),
                    types[rnd.nextInt(types.length)],
                    severities[rnd.nextInt(severities.length)],
                    base.plusSeconds(i),
                    null,
                    rnd.nextDouble(100),
                    "synthetic benchmark event");
            current.add(event);
            if (current.size() == windowSize) {
                windows.add(current);
                current = new ArrayList<>(windowSize);
            }
        }
        if (!current.isEmpty()) {
            windows.add(current);
        }
        return windows;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
