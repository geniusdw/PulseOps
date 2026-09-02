package com.pulseops.topology;

import com.pulseops.common.error.ResourceNotFoundException;
import com.pulseops.topology.model.ServiceDependency;
import com.pulseops.topology.model.ServiceNode;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read model over the service topology.
 *
 * <p>The topology is tiny (a handful of nodes) and effectively static once
 * seeded, so the whole graph is cached in memory as adjacency maps and rebuilt
 * only when {@link #invalidate()} is called. The correlation engine calls
 * {@link #dependencyDistance} on every candidate event pair, so this must be an
 * in-memory lookup, not a query per call.
 */
@Service
public class TopologyService {

    private final ServiceNodeRepository nodeRepository;
    private final ServiceDependencyRepository dependencyRepository;

    /** Directed: service -> services it directly depends on. */
    private volatile Map<String, Set<String>> downstream = Map.of();
    /** Directed: service -> services that directly depend on it. */
    private volatile Map<String, Set<String>> upstream = Map.of();
    /** Undirected adjacency, for shortest-path distance. */
    private volatile Map<String, Set<String>> undirected = Map.of();

    public TopologyService(ServiceNodeRepository nodeRepository,
                           ServiceDependencyRepository dependencyRepository) {
        this.nodeRepository = nodeRepository;
        this.dependencyRepository = dependencyRepository;
    }

    /** Rebuild the in-memory graph from the database. */
    public synchronized void invalidate() {
        Map<String, Set<String>> down = new HashMap<>();
        Map<String, Set<String>> up = new HashMap<>();
        Map<String, Set<String>> undir = new HashMap<>();

        for (ServiceNode node : nodeRepository.findAll()) {
            down.computeIfAbsent(node.getName(), k -> new HashSet<>());
            up.computeIfAbsent(node.getName(), k -> new HashSet<>());
            undir.computeIfAbsent(node.getName(), k -> new HashSet<>());
        }
        for (ServiceDependency edge : dependencyRepository.findAll()) {
            down.computeIfAbsent(edge.getSource(), k -> new HashSet<>()).add(edge.getTarget());
            up.computeIfAbsent(edge.getTarget(), k -> new HashSet<>()).add(edge.getSource());
            undir.computeIfAbsent(edge.getSource(), k -> new HashSet<>()).add(edge.getTarget());
            undir.computeIfAbsent(edge.getTarget(), k -> new HashSet<>()).add(edge.getSource());
        }
        this.downstream = down;
        this.upstream = up;
        this.undirected = undir;
    }

    private Map<String, Set<String>> undirected() {
        if (undirected.isEmpty()) {
            invalidate();
        }
        return undirected;
    }

    public Set<String> allServiceNames() {
        return Collections.unmodifiableSet(undirected().keySet());
    }

    public boolean exists(String service) {
        return undirected().containsKey(service);
    }

    public List<ServiceNode> allNodes() {
        return nodeRepository.findAll();
    }

    public ServiceNode requireNode(String name) {
        return nodeRepository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SERVICE_NOT_FOUND", "Service '" + name + "' was not found"));
    }

    public List<String> directDependenciesOf(String service) {
        requireNode(service);
        return sortedList(downstream.getOrDefault(service, Set.of()));
    }

    public List<String> directDependentsOf(String service) {
        requireNode(service);
        return sortedList(upstream.getOrDefault(service, Set.of()));
    }

    /**
     * Shortest number of dependency hops between two services in the
     * <em>undirected</em> dependency graph.
     *
     * @return {@code 0} if the services are the same, {@code 1} for a direct
     *         dependency in either direction, larger for transitive links, and
     *         {@code -1} if either service is unknown or they are unreachable.
     */
    public int dependencyDistance(String a, String b) {
        Map<String, Set<String>> graph = undirected();
        if (!graph.containsKey(a) || !graph.containsKey(b)) {
            return -1;
        }
        if (a.equals(b)) {
            return 0;
        }
        Set<String> visited = new HashSet<>();
        Deque<String> frontier = new ArrayDeque<>();
        Map<String, Integer> depth = new HashMap<>();
        frontier.add(a);
        visited.add(a);
        depth.put(a, 0);
        while (!frontier.isEmpty()) {
            String current = frontier.poll();
            int d = depth.get(current);
            for (String neighbour : graph.getOrDefault(current, Set.of())) {
                if (neighbour.equals(b)) {
                    return d + 1;
                }
                if (visited.add(neighbour)) {
                    depth.put(neighbour, d + 1);
                    frontier.add(neighbour);
                }
            }
        }
        return -1;
    }

    private static List<String> sortedList(Set<String> set) {
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }
}
