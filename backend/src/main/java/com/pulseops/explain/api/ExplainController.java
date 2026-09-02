package com.pulseops.explain.api;

import com.pulseops.explain.IncidentExplanation;
import com.pulseops.explain.IncidentExplanationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optional AI-style explanation layer.
 *
 * <p>{@code POST /api/incidents/{id}/explain} returns a validated, structured
 * explanation built from the deterministic incident data. With the default
 * {@code MockIncidentExplainer} it needs no credentials.
 */
@RestController
@RequestMapping("/api/incidents")
public class ExplainController {

    private final IncidentExplanationService explanationService;

    public ExplainController(IncidentExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    @PostMapping("/{id}/explain")
    public IncidentExplanation explain(@PathVariable String id) {
        return explanationService.explain(id);
    }
}
