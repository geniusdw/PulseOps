package com.pulseops.incident;

import com.pulseops.correlation.model.SignalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serialises all incident reconciliation.
 *
 * <p>Many worker threads process events in parallel, but incident grouping reads
 * the current incident graph, mutates it, and writes it back — a
 * read-modify-write over shared state. Rather than sprinkle row locks and risk
 * deadlock between merges, PulseOps v1 funnels reconciliation through one
 * {@link ReentrantLock}: workers persist events concurrently, then take turns
 * updating incidents. This is simple, correct, and fast enough because
 * reconciliation is cheap relative to ingestion. The README explains how this
 * becomes a partitioned/actor model when it needs to scale.
 */
@Component
public class IncidentReconciliationCoordinator {

    private static final Logger log = LoggerFactory.getLogger(IncidentReconciliationCoordinator.class);

    private final IncidentManager incidentManager;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    public IncidentReconciliationCoordinator(IncidentManager incidentManager, Clock clock) {
        this.incidentManager = incidentManager;
        this.clock = clock;
    }

    /** Called by a worker after it persists a batch of events. */
    public void onBatchProcessed(List<SignalEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }
        Instant earliest = batch.stream()
                .map(SignalEvent::occurredAt)
                .min(Comparator.naturalOrder())
                .orElse(clock.instant());
        runReconcile(earliest);
    }

    /** Called by the scheduled safety-net sweep. */
    public void sweep() {
        runReconcile(clock.instant());
    }

    private void runReconcile(Instant anchor) {
        lock.lock();
        try {
            incidentManager.reconcile(anchor);
        } catch (RuntimeException ex) {
            log.error("Reconciliation failed for anchor {}", anchor, ex);
        } finally {
            lock.unlock();
        }
    }
}
