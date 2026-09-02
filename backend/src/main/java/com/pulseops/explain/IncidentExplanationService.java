package com.pulseops.explain;

import com.pulseops.incident.IncidentQueryService;
import com.pulseops.incident.api.IncidentDetailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds the structured context for an incident, asks the configured
 * {@link IncidentExplainer} for prose, validates the result, and returns it.
 */
@Service
public class IncidentExplanationService {

    private static final Logger log = LoggerFactory.getLogger(IncidentExplanationService.class);

    private final IncidentQueryService incidentQueryService;
    private final IncidentExplainer explainer;
    private final ExplanationValidator validator;

    public IncidentExplanationService(IncidentQueryService incidentQueryService,
                                      IncidentExplainer explainer,
                                      ExplanationValidator validator) {
        this.incidentQueryService = incidentQueryService;
        this.explainer = explainer;
        this.validator = validator;
    }

    public IncidentExplanation explain(String incidentPublicId) {
        IncidentDetailDto detail = incidentQueryService.detail(incidentPublicId);

        IncidentExplanationContext context = new IncidentExplanationContext(
                detail.incidentId(),
                detail.title(),
                detail.severity(),
                detail.status(),
                detail.affectedServices(),
                detail.correlationSummary(),
                detail.rootCauses(),
                detail.timeline());

        IncidentExplanation explanation = explainer.explain(context);
        validator.validate(explanation, context);
        log.info("Generated explanation for {} via {}", detail.incidentId(), explainer.name());
        return explanation;
    }
}
