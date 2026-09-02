package com.pulseops.explain;

import java.util.List;

/**
 * The structured schema an incident explanation must conform to, whoever
 * produces it (mock or LLM). The API never returns free-form model text — it
 * returns this validated object.
 *
 * @param summary          2–4 sentence natural-language description
 * @param probableCause    the single most likely cause, in a phrase
 * @param evidence         concrete observations supporting the conclusion
 * @param recommendedChecks next diagnostic steps for an on-call engineer
 * @param generatedBy      "mock" or the model id, for transparency
 */
public record IncidentExplanation(
        String summary,
        String probableCause,
        List<String> evidence,
        List<String> recommendedChecks,
        String generatedBy
) {
}
