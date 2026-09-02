package com.pulseops.topology.api;

import com.pulseops.topology.TopologyService;
import com.pulseops.topology.model.ServiceDependency;
import com.pulseops.topology.model.ServiceNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoints backing the React "Service Map" page and used by the
 * correlation-explanation UI to show dependency paths.
 */
@RestController
@RequestMapping("/api/services")
public class TopologyController {

    private final TopologyService topology;
    private final com.pulseops.topology.ServiceDependencyRepository dependencyRepository;

    public TopologyController(TopologyService topology,
                              com.pulseops.topology.ServiceDependencyRepository dependencyRepository) {
        this.topology = topology;
        this.dependencyRepository = dependencyRepository;
    }

    /** Whole graph: nodes + edges, for a single-request render. */
    @GetMapping
    public TopologyGraphDto graph() {
        List<ServiceDto> services = topology.allNodes().stream()
                .map(this::toDto)
                .toList();
        List<DependencyEdgeDto> edges = dependencyRepository.findAll().stream()
                .map(this::toEdge)
                .toList();
        return new TopologyGraphDto(services, edges);
    }

    @GetMapping("/{name}")
    public ServiceDto byName(@PathVariable String name) {
        return toDto(topology.requireNode(name));
    }

    @GetMapping("/{name}/dependencies")
    public ServiceDto dependencies(@PathVariable String name) {
        return toDto(topology.requireNode(name));
    }

    private ServiceDto toDto(ServiceNode node) {
        return ServiceDto.of(node,
                topology.directDependenciesOf(node.getName()),
                topology.directDependentsOf(node.getName()));
    }

    private DependencyEdgeDto toEdge(ServiceDependency edge) {
        return new DependencyEdgeDto(edge.getSource(), edge.getTarget());
    }
}
