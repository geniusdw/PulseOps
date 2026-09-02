package com.pulseops.events;

import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a JPA {@link Specification} from optional filters for
 * {@code GET /api/events}. Only the filters the caller supplied become
 * predicates, so there is one code path regardless of filter combination.
 */
public record EventQuery(
        String service,
        Severity severity,
        EventType eventType,
        Instant from,
        Instant to
) {

    public Specification<EventEntity> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (service != null && !service.isBlank()) {
                predicates.add(cb.equal(root.get("service"), service));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
