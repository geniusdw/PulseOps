package com.pulseops.simulator.api;

import com.pulseops.common.error.ResourceNotFoundException;
import com.pulseops.simulator.Scenario;
import com.pulseops.simulator.ScenarioRunResult;
import com.pulseops.simulator.SimulatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Drives the event simulator from the dashboard.
 *
 * <ul>
 *   <li>{@code GET  /api/simulator/scenarios} — list available scenarios</li>
 *   <li>{@code POST /api/simulator/scenarios/{slug}} — inject that scenario</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @GetMapping("/scenarios")
    public List<ScenarioInfo> scenarios() {
        return Arrays.stream(Scenario.values())
                .map(s -> new ScenarioInfo(s.slug(), s.name(), s.description()))
                .toList();
    }

    @PostMapping("/scenarios/{slug}")
    public ScenarioRunResult run(@PathVariable String slug) {
        Scenario scenario = Scenario.fromSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SCENARIO_NOT_FOUND", "Unknown scenario '" + slug + "'"));
        return simulatorService.run(scenario);
    }

    public record ScenarioInfo(String slug, String name, String description) {
    }
}
