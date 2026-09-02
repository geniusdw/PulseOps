package com.pulseops.explain;

/**
 * Produces a human-readable explanation of an incident from its structured
 * context.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link MockIncidentExplainer} — deterministic, no external calls, the
 *       default so the project runs with zero credentials.</li>
 *   <li>A future {@code LlmIncidentExplainer} would build a prompt from the same
 *       {@link IncidentExplanationContext}, call a model, parse the JSON response
 *       into {@link IncidentExplanation} and let {@link ExplanationValidator}
 *       reject anything malformed before it reaches the API.</li>
 * </ul>
 * The deterministic engine remains the source of truth in both cases.
 */
public interface IncidentExplainer {

    IncidentExplanation explain(IncidentExplanationContext context);

    /** Identifier surfaced to the UI ("mock", or a model id). */
    String name();
}
