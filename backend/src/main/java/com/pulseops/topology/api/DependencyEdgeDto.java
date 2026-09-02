package com.pulseops.topology.api;

/** One directed edge for the React service-map graph. */
public record DependencyEdgeDto(String source, String target) {
}
