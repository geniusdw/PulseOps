package com.pulseops.explain;

import com.pulseops.incident.api.RootCauseDto;
import com.pulseops.incident.timeline.TimelineEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic explainer. It composes prose from the structured context using
 * templates — no network, no model, no credentials. Because it is deterministic
 * it is also trivially testable and safe to run in CI.
 *
 * <p>{@code @ConditionalOnMissingBean} means dropping in a real
 * {@code LlmIncidentExplainer} bean automatically replaces this one.
 */
@Component
@ConditionalOnMissingBean(name = "llmIncidentExplainer")
public class MockIncidentExplainer implements IncidentExplainer {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public IncidentExplanation explain(IncidentExplanationContext ctx) {
        RootCauseDto top = ctx.rootCauses().isEmpty() ? null : ctx.rootCauses().get(0);
        String services = String.join(", ", ctx.affectedServices());

        String firstSignal = ctx.timeline().stream()
                .filter(t -> "EVENT".equals(t.kind()))
                .findFirst()
                .map(t -> t.title())
                .orElse("the first observed anomaly");

        StringBuilder summary = new StringBuilder()
                .append("Incident ").append(ctx.incidentId())
                .append(" (").append(ctx.severity()).append(") affects ").append(services).append(". ");
        if (top != null) {
            summary.append("The most likely cause is ").append(lower(top.label()))
                    .append(" (heuristic score ").append(pct(top.score())).append("). ");
        }
        summary.append("It began with ").append(lower(firstSignal))
                .append("; ").append(lower(ctx.correlationSummary() == null ? "related events followed" : ctx.correlationSummary()));

        List<String> evidence = new ArrayList<>();
        if (top != null) {
            evidence.addAll(top.evidence());
        }
        for (TimelineEntry entry : ctx.timeline()) {
            if ("EVENT".equals(entry.kind())) {
                evidence.add(entry.title() + " — " + entry.detail());
            }
            if (evidence.size() >= 6) {
                break;
            }
        }

        List<String> checks = recommendedChecks(top, ctx.affectedServices());

        return new IncidentExplanation(
                summary.toString().trim(),
                top != null ? top.label() : "Undetermined",
                evidence,
                checks,
                name());
    }

    private List<String> recommendedChecks(RootCauseDto top, List<String> services) {
        if (top == null) {
            return List.of("Review the event timeline for the earliest anomaly and its service.");
        }
        return switch (top.type()) {
            case "DATABASE_FAILURE" -> List.of(
                    "Check database connection pool utilisation and max connections",
                    "Look for long-running or blocking queries",
                    "Confirm no schema migration or vacuum is running");
            case "DEPLOYMENT" -> List.of(
                    "Compare error rates before and after the deployment",
                    "Consider rolling back the most recent deployment",
                    "Review the deployment's changelog for risky changes");
            case "RESOURCE_EXHAUSTION" -> List.of(
                    "Check CPU / memory usage on the affected hosts",
                    "Look for a traffic spike or a runaway background job",
                    "Verify autoscaling triggered as expected");
            case "NETWORK_PROBLEM" -> List.of(
                    "Check connectivity and DNS between the affected services",
                    "Review load balancer / service mesh health",
                    "Look for packet loss or elevated connection timeouts");
            case "SERVICE_FAILURE" -> List.of(
                    "Inspect logs on " + String.join(", ", services) + " around the incident start",
                    "Check for crash loops / restart counts",
                    "Verify downstream dependencies are healthy");
            default -> List.of(
                    "Correlate the timeline with recent changes",
                    "Inspect the earliest affected service in depth");
        };
    }

    private static String lower(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        // Leave identifier-style tokens (DB_CONNECTION_EXHAUSTION, HTTP_500) alone.
        String firstWord = s.split("\\s", 2)[0];
        if (firstWord.equals(firstWord.toUpperCase()) && firstWord.length() > 1) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String pct(double score) {
        return Math.round(score * 100) + "%";
    }
}
