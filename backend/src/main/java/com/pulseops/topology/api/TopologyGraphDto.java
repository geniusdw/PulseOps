package com.pulseops.topology.api;

import java.util.List;

/** Whole-graph payload the Service Map page renders in one request. */
public record TopologyGraphDto(List<ServiceDto> services, List<DependencyEdgeDto> edges) {
}
