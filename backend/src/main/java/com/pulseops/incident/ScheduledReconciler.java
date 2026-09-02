package com.pulseops.incident;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net. The queue-driven pipeline is the fast path, but if the queue ever
 * rejects an event (backpressure) or a worker batch fails, that persisted event
 * would otherwise never be correlated. This periodic sweep re-runs
 * reconciliation over the recent window so no stored event is left behind.
 *
 * <p>It shares the coordinator's lock, so it never races the workers.
 */
@Component
public class ScheduledReconciler {

    private final IncidentReconciliationCoordinator coordinator;

    public ScheduledReconciler(IncidentReconciliationCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${pulseops.reconcile.sweep-interval-ms:15000}")
    public void sweep() {
        coordinator.sweep();
    }
}
