package com.pulseops.simulator;

import com.pulseops.events.EventIngestionService;
import com.pulseops.events.NormalizedEvent;
import com.pulseops.events.model.EventEntity;
import com.pulseops.incident.IncidentReconciliationCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a scenario script: ingests its events through the <em>real</em> ingestion
 * path (so they are validated, persisted, queued and correlated exactly like API
 * traffic), then nudges reconciliation so an incident appears without waiting for
 * the periodic sweep.
 *
 * <p>Events are back-dated so the whole scenario sits within the last few minutes
 * and therefore inside the correlation window.
 */
@Service
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);

    /** Start the scenario this far in the past so all events are recent history. */
    private static final int BACKDATE_MINUTES = 3;

    private final ScenarioGenerator generator;
    private final EventIngestionService ingestionService;
    private final IncidentReconciliationCoordinator coordinator;
    private final Clock clock;

    public SimulatorService(ScenarioGenerator generator,
                            EventIngestionService ingestionService,
                            IncidentReconciliationCoordinator coordinator,
                            Clock clock) {
        this.generator = generator;
        this.ingestionService = ingestionService;
        this.coordinator = coordinator;
        this.clock = clock;
    }

    public ScenarioRunResult run(Scenario scenario) {
        List<PlannedEvent> plan = generator.plan(scenario);
        Instant start = clock.instant().minus(BACKDATE_MINUTES, ChronoUnit.MINUTES);

        List<String> eventIds = new ArrayList<>();
        Long deploymentId = null;

        for (PlannedEvent planned : plan) {
            Instant occurredAt = start.plusSeconds(planned.offsetSeconds());
            NormalizedEvent normalized = new NormalizedEvent(
                    occurredAt,
                    planned.service(),
                    "sim-host",
                    planned.type(),
                    planned.severity(),
                    planned.metric(),
                    planned.value(),
                    planned.message(),
                    planned.version());

            Long linkId = planned.linkToDeployment() ? deploymentId : null;
            EventEntity saved = ingestionService.ingestNormalized(normalized, linkId);
            eventIds.add(saved.getPublicId());

            if (saved.getDeploymentId() != null && planned.type().name().equals("DEPLOYMENT")) {
                deploymentId = saved.getDeploymentId();
            }
        }

        // Prompt the coordinator so the incident is visible immediately.
        coordinator.sweep();

        log.info("Scenario {} injected {} events", scenario.slug(), eventIds.size());
        return new ScenarioRunResult(
                scenario.slug(),
                scenario.description(),
                eventIds.size(),
                eventIds,
                deploymentId == null ? null : "DEP-" + deploymentId,
                noteFor(scenario));
    }

    private static String noteFor(Scenario scenario) {
        return scenario == Scenario.NORMAL_TRAFFIC
                ? "These events are low-severity and spread out; no incident should be created."
                : "Correlated events should appear as a single incident within a few seconds.";
    }
}
