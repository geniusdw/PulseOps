package com.pulseops.explain;

import com.pulseops.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExplanationValidatorTest {

    private final ExplanationValidator validator = new ExplanationValidator();

    private final IncidentExplanationContext context = new IncidentExplanationContext(
            "INC-1", "Title", "HIGH", "OPEN", List.of("payment-api"),
            "summary", List.of(), List.of());

    @Test
    void acceptsAWellFormedExplanation() {
        IncidentExplanation ok = new IncidentExplanation(
                "A concise summary.", "Database failure",
                List.of("evidence one"), List.of("check the pool"), "mock");

        assertThatCode(() -> validator.validate(ok, context)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptySummary() {
        IncidentExplanation bad = new IncidentExplanation(
                "  ", "cause", List.of("e"), List.of("c"), "mock");

        assertThatThrownBy(() -> validator.validate(bad, context))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEmptyEvidenceList() {
        IncidentExplanation bad = new IncidentExplanation(
                "summary", "cause", List.of(), List.of("c"), "mock");

        assertThatThrownBy(() -> validator.validate(bad, context))
                .isInstanceOf(ValidationException.class);
    }
}
