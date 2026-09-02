package com.pulseops.incident.rootcause;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.events.model.EventType;
import com.pulseops.topology.TopologyService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ranks probable root causes for an incident using explainable heuristic rules.
 *
 * <p><strong>These are heuristic "root-cause scores", not statistical
 * probabilities.</strong> Each rule adds to a candidate's raw score and records
 * a piece of evidence; the raw score is then squashed into {@code [0,1)} with a
 * diminishing-returns curve so multiple weak signals cannot exceed one strong
 * one by much.
 */
@Component
public class RootCauseRanker {

    /** Raw score at which the squashed score reaches 0.5. */
    private static final double HALF_SATURATION = 0.6;
    private static final int MAX_CANDIDATES = 5;

    private final TopologyService topology;

    public RootCauseRanker(TopologyService topology) {
        this.topology = topology;
    }

    public List<RootCauseCandidate> rank(List<SignalEvent> events, List<DeploymentEntity> deployments) {
        if (events.isEmpty()) {
            return List.of();
        }

        Set<EventType> types = events.stream().map(SignalEvent::eventType).collect(Collectors.toSet());
        List<String> services = events.stream().map(SignalEvent::service).distinct().toList();
        String dominantService = mode(events.stream().map(SignalEvent::service).toList());
        SignalEvent earliestAnomaly = events.stream()
                .filter(e -> e.eventType() != EventType.DEPLOYMENT)
                .min(Comparator.comparing(SignalEvent::occurredAt))
                .orElse(events.get(0));

        Map<RootCauseType, Accumulator> acc = new EnumMap<>(RootCauseType.class);
        for (RootCauseType t : RootCauseType.values()) {
            acc.put(t, new Accumulator());
        }

        scoreDeployment(acc.get(RootCauseType.DEPLOYMENT), types, deployments, earliestAnomaly);
        scoreDatabase(acc.get(RootCauseType.DATABASE_FAILURE), types, services);
        scoreResource(acc.get(RootCauseType.RESOURCE_EXHAUSTION), types);
        scoreNetwork(acc.get(RootCauseType.NETWORK_PROBLEM), types, services.size());
        scoreServiceFailure(acc.get(RootCauseType.SERVICE_FAILURE), types, services.size(), dominantService);
        scoreDependency(acc.get(RootCauseType.DEPENDENCY_FAILURE), types, services, earliestAnomaly);

        List<RootCauseCandidate> candidates = new ArrayList<>();
        acc.forEach((type, a) -> {
            if (a.raw > 0) {
                candidates.add(new RootCauseCandidate(
                        type,
                        labelFor(type, deployments, dominantService, types),
                        squash(a.raw),
                        List.copyOf(a.evidence)));
            }
        });
        candidates.sort(Comparator.comparingDouble(RootCauseCandidate::score).reversed());
        return candidates.size() > MAX_CANDIDATES ? candidates.subList(0, MAX_CANDIDATES) : candidates;
    }

    private void scoreDeployment(Accumulator a, Set<EventType> types,
                                 List<DeploymentEntity> deployments, SignalEvent earliestAnomaly) {
        boolean hasDeployEvent = types.contains(EventType.DEPLOYMENT);
        if (hasDeployEvent || !deployments.isEmpty()) {
            a.add(0.7, "a deployment occurred within the incident window");
        }
        Optional<DeploymentEntity> preceding = deployments.stream()
                .filter(d -> d.getDeployedAt().isBefore(earliestAnomaly.occurredAt()))
                .max(Comparator.comparing(DeploymentEntity::getDeployedAt));
        preceding.ifPresent(d -> {
            long gap = Duration.between(d.getDeployedAt(), earliestAnomaly.occurredAt()).toSeconds();
            a.add(0.3, "deployment to " + d.getService() + " preceded the first anomaly by " + gap + "s");
        });
        if (types.contains(EventType.SERVICE_RESTART)) {
            a.add(0.15, "a service restart followed the change");
        }
    }

    private void scoreDatabase(Accumulator a, Set<EventType> types, List<String> services) {
        if (types.contains(EventType.DB_CONNECTION_EXHAUSTION)) {
            a.add(0.7, "DB_CONNECTION_EXHAUSTION observed");
        }
        if (services.contains("database")) {
            a.add(0.25, "the database service emitted events");
        }
        if (types.contains(EventType.HTTP_500) || types.contains(EventType.PAYMENT_FAILURE)) {
            a.add(0.15, "downstream 5xx / payment failures are consistent with a database problem");
        }
    }

    private void scoreResource(Accumulator a, Set<EventType> types) {
        if (types.contains(EventType.CPU_SPIKE)) {
            a.add(0.5, "CPU_SPIKE observed");
        }
        if (types.contains(EventType.MEMORY_SPIKE)) {
            a.add(0.5, "MEMORY_SPIKE observed");
        }
        if (types.contains(EventType.HIGH_LATENCY)) {
            a.add(0.2, "elevated latency is consistent with resource saturation");
        }
        if (types.contains(EventType.HTTP_503)) {
            a.add(0.2, "HTTP_503 (service overloaded) observed");
        }
        if (types.contains(EventType.QUEUE_BACKLOG)) {
            a.add(0.15, "queue backlog observed");
        }
    }

    private void scoreNetwork(Accumulator a, Set<EventType> types, int serviceCount) {
        if (types.contains(EventType.NETWORK_ERROR)) {
            a.add(0.7, "NETWORK_ERROR observed");
        }
        if (types.contains(EventType.HTTP_503)) {
            a.add(0.2, "HTTP_503 is consistent with connectivity loss");
        }
        if (serviceCount >= 3) {
            a.add(0.1, "impact spans " + serviceCount + " services simultaneously");
        }
    }

    private void scoreServiceFailure(Accumulator a, Set<EventType> types, int serviceCount, String dominantService) {
        if (types.contains(EventType.SERVICE_RESTART)) {
            a.add(0.45, "SERVICE_RESTART observed");
        }
        if (serviceCount == 1) {
            a.add(0.25, "activity is confined to a single service (" + dominantService + ")");
        }
        if (types.contains(EventType.HTTP_500)) {
            a.add(0.15, "HTTP_500 errors observed");
        }
    }

    private void scoreDependency(Accumulator a, Set<EventType> types,
                                 List<String> services, SignalEvent earliestAnomaly) {
        if (services.size() >= 3) {
            a.add(0.35, "events span " + services.size() + " services along a dependency path");
        }
        List<String> dependents = topology.exists(earliestAnomaly.service())
                ? topology.directDependentsOf(earliestAnomaly.service())
                : List.of();
        boolean upstreamFirst = dependents.stream().anyMatch(services::contains);
        if (upstreamFirst) {
            a.add(0.3, "earliest activity was on " + earliestAnomaly.service()
                    + ", upstream of other affected services");
        }
        if (types.contains(EventType.DB_CONNECTION_EXHAUSTION) && services.size() > 1) {
            a.add(0.15, "a shared dependency (database) is under stress");
        }
    }

    private String labelFor(RootCauseType type, List<DeploymentEntity> deployments,
                            String dominantService, Set<EventType> types) {
        return switch (type) {
            case DEPLOYMENT -> deployments.isEmpty()
                    ? "Deployment"
                    : "Deployment " + deployments.get(deployments.size() - 1).getPublicId()
                    + " (" + deployments.get(deployments.size() - 1).getService() + ")";
            case SERVICE_FAILURE -> "Service failure (" + dominantService + ")";
            case RESOURCE_EXHAUSTION -> {
                if (types.contains(EventType.MEMORY_SPIKE) && !types.contains(EventType.CPU_SPIKE)) {
                    yield "Resource exhaustion (memory)";
                }
                if (types.contains(EventType.CPU_SPIKE) && !types.contains(EventType.MEMORY_SPIKE)) {
                    yield "Resource exhaustion (CPU)";
                }
                yield "Resource exhaustion";
            }
            default -> type.label();
        };
    }

    /** Diminishing-returns squash: raw 0.6 -> 0.5, raw 1.2 -> 0.75, raw 1.8 -> 0.875. */
    private static double squash(double raw) {
        return 1.0 - Math.pow(0.5, raw / HALF_SATURATION);
    }

    private static <T> T mode(List<T> values) {
        return values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(values.get(0));
    }

    private static final class Accumulator {
        private double raw = 0.0;
        private final List<String> evidence = new ArrayList<>();

        void add(double amount, String reason) {
            raw += amount;
            evidence.add(reason);
        }
    }
}
