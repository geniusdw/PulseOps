package com.pulseops.incident;

import com.pulseops.correlation.CorrelationEngine;
import com.pulseops.correlation.CorrelationProperties;
import com.pulseops.correlation.model.EventCluster;
import com.pulseops.correlation.model.PairCorrelation;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.deployments.DeploymentRepository;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.events.EventRepository;
import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.Severity;
import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.model.IncidentEventLink;
import com.pulseops.incident.model.IncidentStatus;
import com.pulseops.incident.rootcause.RootCauseCandidate;
import com.pulseops.incident.rootcause.RootCauseRanker;
import com.pulseops.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns correlation output into persisted incidents.
 *
 * <p>For each event cluster it either <em>creates</em> a new incident, <em>grows</em>
 * an existing open incident that already contains one of the cluster's events,
 * or <em>merges</em> multiple open incidents that the cluster now bridges. After
 * attaching events it recomputes the incident's derived fields (severity,
 * confidence, probable root cause, title, timeline anchor).
 *
 * <p>Called only from {@code IncidentReconciliationCoordinator}, which holds a
 * process-wide lock, so this class assumes single-writer access to incident
 * state. That is a deliberate v1 simplification (see README "Concurrency").
 */
@Component
public class IncidentManager {

    private static final Logger log = LoggerFactory.getLogger(IncidentManager.class);

    private final EventRepository eventRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventLinkRepository linkRepository;
    private final DeploymentRepository deploymentRepository;
    private final CorrelationEngine engine;
    private final SeverityCalculator severityCalculator;
    private final RootCauseRanker rootCauseRanker;
    private final IncidentTitleFactory titleFactory;
    private final TopologyService topology;
    private final CorrelationProperties correlationProps;
    private final Clock clock;

    public IncidentManager(EventRepository eventRepository,
                           IncidentRepository incidentRepository,
                           IncidentEventLinkRepository linkRepository,
                           DeploymentRepository deploymentRepository,
                           CorrelationEngine engine,
                           SeverityCalculator severityCalculator,
                           RootCauseRanker rootCauseRanker,
                           IncidentTitleFactory titleFactory,
                           TopologyService topology,
                           CorrelationProperties correlationProps,
                           Clock clock) {
        this.eventRepository = eventRepository;
        this.incidentRepository = incidentRepository;
        this.linkRepository = linkRepository;
        this.deploymentRepository = deploymentRepository;
        this.engine = engine;
        this.severityCalculator = severityCalculator;
        this.rootCauseRanker = rootCauseRanker;
        this.titleFactory = titleFactory;
        this.topology = topology;
        this.correlationProps = correlationProps;
        this.clock = clock;
    }

    @Transactional
    public void reconcile(Instant windowStart) {
        Instant now = clock.instant();
        Instant from = windowStart.minus(correlationProps.windowMinutes(), ChronoUnit.MINUTES);

        List<EventEntity> windowEvents =
                eventRepository.findByOccurredAtBetweenOrderByOccurredAtAsc(from, now);
        if (windowEvents.size() < 2) {
            return;
        }
        List<SignalEvent> signals = windowEvents.stream().map(SignalEvent::from).toList();
        List<EventCluster> clusters = engine.correlate(signals);
        if (clusters.isEmpty()) {
            return;
        }

        List<IncidentEntity> openIncidents = incidentRepository.findByStatusNot(IncidentStatus.RESOLVED);
        Map<Long, IncidentEntity> openById = new HashMap<>();
        openIncidents.forEach(i -> openById.put(i.getId(), i));

        Map<Long, Long> eventToIncident = new HashMap<>();
        if (!openById.isEmpty()) {
            for (IncidentEventLink link : linkRepository.findByIncidentIdIn(openById.keySet())) {
                eventToIncident.put(link.getEventId(), link.getIncidentId());
            }
        }

        for (EventCluster cluster : clusters) {
            Set<Long> matched = new LinkedHashSet<>();
            for (Long eventId : cluster.eventIds()) {
                Long incidentId = eventToIncident.get(eventId);
                if (incidentId != null) {
                    matched.add(incidentId);
                }
            }

            IncidentEntity target;
            if (matched.isEmpty()) {
                target = incidentRepository.save(new IncidentEntity(
                        "Correlated incident", Severity.LOW, cluster.startedAt(), now));
                openById.put(target.getId(), target);
                log.info("Opened incident {} from cluster of {} events",
                        target.getPublicId(), cluster.events().size());
            } else {
                List<Long> ordered = matched.stream().sorted().toList();
                target = openById.get(ordered.get(0));
                for (Long other : ordered.subList(1, ordered.size())) {
                    mergeInto(target.getId(), other, eventToIncident, openById);
                }
            }

            attachEvents(target, cluster, eventToIncident);
            recompute(target.getId(), now);
        }
    }

    private void attachEvents(IncidentEntity target, EventCluster cluster, Map<Long, Long> eventToIncident) {
        for (SignalEvent event : cluster.events()) {
            if (linkRepository.existsByIncidentIdAndEventId(target.getId(), event.id())) {
                continue;
            }
            linkRepository.save(new IncidentEventLink(
                    target.getId(), event.id(), pairScoreFor(cluster, event)));
            eventToIncident.put(event.id(), target.getId());
        }
    }

    private void mergeInto(Long targetId, Long otherId,
                           Map<Long, Long> eventToIncident, Map<Long, IncidentEntity> openById) {
        for (IncidentEventLink link : linkRepository.findByIncidentId(otherId)) {
            if (linkRepository.existsByIncidentIdAndEventId(targetId, link.getEventId())) {
                linkRepository.delete(link);
            } else {
                link.setIncidentId(targetId);
                linkRepository.save(link);
            }
            eventToIncident.put(link.getEventId(), targetId);
        }
        incidentRepository.deleteById(otherId);
        openById.remove(otherId);
        log.info("Merged incident INC-{} into INC-{}", otherId, targetId);
    }

    /** Recompute all derived fields of an incident from its current event set. */
    private void recompute(Long incidentId, Instant now) {
        IncidentEntity incident = incidentRepository.findById(incidentId).orElseThrow();
        List<Long> eventIds = linkRepository.findByIncidentId(incidentId).stream()
                .map(IncidentEventLink::getEventId)
                .toList();
        List<SignalEvent> events = new ArrayList<>(
                eventRepository.findAllById(eventIds).stream().map(SignalEvent::from).toList());
        if (events.isEmpty()) {
            return;
        }
        events.sort(Comparator.comparing(SignalEvent::occurredAt));

        Severity severity = severityCalculator.calculate(events);
        List<DeploymentEntity> deployments = relevantDeployments(events);
        List<RootCauseCandidate> causes = rootCauseRanker.rank(events, deployments);

        List<EventCluster> fresh = engine.correlate(events);
        double strength = fresh.isEmpty() ? incident.getConfidenceScore() : fresh.get(0).strength();
        String summary = fresh.isEmpty() ? incident.getCorrelationSummary()
                : fresh.get(0).explanation().summary();

        incident.setSeverity(severity);
        incident.setStartedAt(events.get(0).occurredAt());
        incident.setTitle(titleFactory.titleFor(events, severity));
        incident.setConfidenceScore(clamp(strength));
        incident.setCorrelationSummary(summary);
        if (!causes.isEmpty()) {
            incident.setProbableRootCause(causes.get(0).label());
            incident.setRootCauseScore(causes.get(0).score());
        }
        incident.setUpdatedAt(now);
        incidentRepository.save(incident);
    }

    /** Deployments near the incident, limited to services in or adjacent to it. */
    public List<DeploymentEntity> relevantDeployments(List<SignalEvent> events) {
        Instant first = events.get(0).occurredAt();
        Instant last = events.get(events.size() - 1).occurredAt();
        Instant lookbackStart = first.minus(correlationProps.deployment().lookbackMinutes(), ChronoUnit.MINUTES);

        Set<String> relevantServices = new HashSet<>();
        for (SignalEvent e : events) {
            relevantServices.add(e.service());
            if (topology.exists(e.service())) {
                relevantServices.addAll(topology.directDependenciesOf(e.service()));
                relevantServices.addAll(topology.directDependentsOf(e.service()));
            }
        }

        return deploymentRepository
                .findByDeployedAtBetweenOrderByDeployedAtAsc(lookbackStart, last)
                .stream()
                .filter(d -> relevantServices.contains(d.getService()))
                .toList();
    }

    private static double pairScoreFor(EventCluster cluster, SignalEvent event) {
        List<Double> touching = cluster.links().stream()
                .filter(l -> l.a().id() == event.id() || l.b().id() == event.id())
                .map(PairCorrelation::score)
                .toList();
        if (touching.isEmpty()) {
            return cluster.strength();
        }
        return touching.stream().mapToDouble(Double::doubleValue).average().orElse(cluster.strength());
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
