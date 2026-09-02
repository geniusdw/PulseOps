package com.pulseops.explain;

import com.pulseops.common.error.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates an {@link IncidentExplanation} before it is returned to a client.
 *
 * <p>For the mock explainer this is belt-and-braces; for a real LLM it is the
 * guardrail that stops hallucinated or malformed output (empty fields, novel
 * services not in the incident, absurd lengths) from reaching the dashboard.
 */
@Component
public class ExplanationValidator {

    private static final int MAX_SUMMARY_LEN = 1200;
    private static final int MAX_ITEMS = 12;
    private static final int MAX_ITEM_LEN = 400;

    public void validate(IncidentExplanation explanation, IncidentExplanationContext context) {
        List<String> problems = new ArrayList<>();

        if (isBlank(explanation.summary())) {
            problems.add("summary is empty");
        } else if (explanation.summary().length() > MAX_SUMMARY_LEN) {
            problems.add("summary exceeds " + MAX_SUMMARY_LEN + " characters");
        }

        if (isBlank(explanation.probableCause())) {
            problems.add("probableCause is empty");
        }

        if (explanation.evidence() == null || explanation.evidence().isEmpty()) {
            problems.add("evidence must contain at least one item");
        } else {
            checkList("evidence", explanation.evidence(), problems);
        }

        if (explanation.recommendedChecks() == null || explanation.recommendedChecks().isEmpty()) {
            problems.add("recommendedChecks must contain at least one item");
        } else {
            checkList("recommendedChecks", explanation.recommendedChecks(), problems);
        }

        if (!problems.isEmpty()) {
            throw new ValidationException(
                    "Generated explanation for " + context.incidentId() + " failed schema validation",
                    problems);
        }
    }

    private void checkList(String field, List<String> items, List<String> problems) {
        if (items.size() > MAX_ITEMS) {
            problems.add(field + " has more than " + MAX_ITEMS + " items");
        }
        for (String item : items) {
            if (isBlank(item)) {
                problems.add(field + " contains an empty item");
            } else if (item.length() > MAX_ITEM_LEN) {
                problems.add(field + " contains an item longer than " + MAX_ITEM_LEN + " characters");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
