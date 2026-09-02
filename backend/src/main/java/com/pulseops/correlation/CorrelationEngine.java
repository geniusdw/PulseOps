package com.pulseops.correlation;

import com.pulseops.correlation.grouping.UnionFind;
import com.pulseops.correlation.model.CorrelationExplanation;
import com.pulseops.correlation.model.EventCluster;
import com.pulseops.correlation.model.PairCorrelation;
import com.pulseops.correlation.model.SignalContribution;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.correlation.score.SubScore;
import com.pulseops.events.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The core of PulseOps. Given the events in a time window it:
 * <ol>
 *   <li>scores every pair with {@link CorrelationScorer},</li>
 *   <li>links pairs whose score clears the threshold,</li>
 *   <li>extracts connected components with {@link UnionFind},</li>
 *   <li>builds an explanation for each component.</li>
 * </ol>
 *
 * <p>Pure domain logic: no Spring MVC, no repositories, no database. It takes a
 * list of immutable {@link SignalEvent}s and returns immutable
 * {@link EventCluster}s, which makes it fast and pleasant to unit test.
 *
 * <p>Complexity is O(n²) in the window size because it is an all-pairs
 * comparison. That is fine for realistic windows (minutes of events); the README
 * discusses blocking/bucketing strategies for larger scales.
 */
@Component
public class CorrelationEngine {

    private static final Logger log = LoggerFactory.getLogger(CorrelationEngine.class);

    /** A cluster must have at least this many events (a lone event is not a correlation). */
    private static final int MIN_CLUSTER_SIZE = 2;

    private final CorrelationScorer scorer;

    public CorrelationEngine(CorrelationScorer scorer) {
        this.scorer = scorer;
    }

    public List<EventCluster> correlate(List<SignalEvent> windowEvents) {
        if (windowEvents.size() < MIN_CLUSTER_SIZE) {
            return List.of();
        }

        List<SignalEvent> events = new ArrayList<>(windowEvents);
        events.sort(Comparator.comparing(SignalEvent::occurredAt).thenComparingLong(SignalEvent::id));

        int n = events.size();
        UnionFind uf = new UnionFind(n);
        List<PairCorrelation> links = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                PairCorrelation pc = scorer.evaluate(events.get(i), events.get(j));
                if (pc.linked()) {
                    uf.union(i, j);
                    links.add(pc);
                }
            }
        }

        List<EventCluster> clusters = new ArrayList<>();
        for (List<Integer> component : uf.components()) {
            if (component.size() < MIN_CLUSTER_SIZE) {
                continue;
            }
            Set<Integer> members = new LinkedHashSet<>(component);
            List<SignalEvent> clusterEvents = component.stream().map(events::get).toList();
            List<PairCorrelation> clusterLinks = links.stream()
                    .filter(link -> members.contains(indexOf(events, link.a()))
                            && members.contains(indexOf(events, link.b())))
                    .toList();
            if (clusterLinks.isEmpty()) {
                continue;
            }
            double strength = clusterLinks.stream().mapToDouble(PairCorrelation::score).average().orElse(0);
            CorrelationExplanation explanation = explain(clusterEvents, clusterLinks, strength);
            clusters.add(new EventCluster(clusterEvents, clusterLinks, strength, explanation));
        }

        clusters.sort(Comparator.comparingDouble(EventCluster::strength).reversed());
        log.debug("Correlated {} events into {} cluster(s)", n, clusters.size());
        return clusters;
    }

    private static int indexOf(List<SignalEvent> events, SignalEvent target) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id() == target.id()) {
                return i;
            }
        }
        return -1;
    }

    private CorrelationExplanation explain(List<SignalEvent> events,
                                           List<PairCorrelation> links,
                                           double strength) {
        long spanSeconds = Duration.between(
                events.get(0).occurredAt(),
                events.get(events.size() - 1).occurredAt()).toSeconds();

        List<String> services = events.stream()
                .map(SignalEvent::service)
                .distinct()
                .sorted()
                .toList();

        Set<String> deploymentIds = new TreeSet<>();
        for (SignalEvent e : events) {
            if (e.deploymentId() != null) {
                deploymentIds.add("DEP-" + e.deploymentId());
            }
            if (e.eventType() == EventType.DEPLOYMENT) {
                deploymentIds.add("DEP-" + e.id());
            }
        }

        // Average each signal's contribution across all linking pairs.
        Map<String, List<SubScore>> bySignal = links.stream()
                .flatMap(l -> l.subScores().stream())
                .collect(Collectors.groupingBy(SubScore::signal));

        List<SignalContribution> contributions = new ArrayList<>();
        bySignal.forEach((signal, subs) -> {
            double avgRaw = subs.stream().mapToDouble(SubScore::rawValue).average().orElse(0);
            double avgWeighted = subs.stream().mapToDouble(SubScore::weightedValue).average().orElse(0);
            List<String> samples = subs.stream()
                    .sorted(Comparator.comparingDouble(SubScore::rawValue).reversed())
                    .map(SubScore::detail)
                    .distinct()
                    .limit(2)
                    .toList();
            contributions.add(new SignalContribution(signal, avgRaw, avgWeighted, samples));
        });
        contributions.sort(Comparator.comparingDouble(SignalContribution::averageWeightedContribution).reversed());

        String topSignalPhrase = contributions.stream()
                .limit(2)
                .map(c -> label(c.signal()))
                .collect(Collectors.joining(" and "));

        StringBuilder summary = new StringBuilder()
                .append(events.size()).append(" events across ")
                .append(String.join(", ", services))
                .append(" within ").append(humanDuration(spanSeconds))
                .append("; strongest signals: ").append(topSignalPhrase);
        if (!deploymentIds.isEmpty()) {
            summary.append("; follows deployment ").append(String.join(", ", deploymentIds));
        }
        summary.append(" (grouping strength ").append(String.format("%.2f", strength)).append(").");

        return new CorrelationExplanation(
                summary.toString(),
                events.size(),
                links.size(),
                spanSeconds,
                services,
                List.copyOf(deploymentIds),
                contributions
        );
    }

    private static String label(String signal) {
        return switch (signal) {
            case "temporal" -> "temporal proximity";
            case "service-dependency" -> "service dependency";
            case "event-type" -> "event-type pattern";
            case "deployment" -> "deployment proximity";
            case "severity" -> "severity";
            default -> signal;
        };
    }

    private static String humanDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long rem = seconds % 60;
        return rem == 0 ? minutes + "m" : minutes + "m " + rem + "s";
    }
}
