package com.pulseops.simulator;

import java.util.List;

public record ScenarioRunResult(
        String scenario,
        String description,
        int eventsCreated,
        List<String> eventIds,
        String deploymentId,
        String note
) {
}
