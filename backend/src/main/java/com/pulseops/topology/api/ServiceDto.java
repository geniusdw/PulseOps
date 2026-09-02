package com.pulseops.topology.api;

import com.pulseops.topology.model.ServiceNode;

import java.util.List;

/**
 * API representation of a service and its immediate topology neighbours.
 * Entities are never returned directly from controllers.
 */
public record ServiceDto(
        String name,
        String displayName,
        String tier,
        String description,
        List<String> dependsOn,
        List<String> dependedOnBy
) {
    public static ServiceDto of(ServiceNode node, List<String> dependsOn, List<String> dependedOnBy) {
        return new ServiceDto(
                node.getName(),
                node.getDisplayName(),
                node.getTier().name(),
                node.getDescription(),
                dependsOn,
                dependedOnBy
        );
    }
}
