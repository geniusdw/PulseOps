package com.pulseops.events;

import com.pulseops.events.model.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

/**
 * {@link JpaSpecificationExecutor} is mixed in so the filterable
 * {@code GET /api/events} query can be built dynamically from whichever filters
 * the caller supplied, without a combinatorial explosion of finder methods.
 */
public interface EventRepository extends JpaRepository<EventEntity, Long>,
        JpaSpecificationExecutor<EventEntity> {

    /** Events in a time window, oldest first — the correlation window scan. */
    List<EventEntity> findByOccurredAtBetweenOrderByOccurredAtAsc(Instant from, Instant to);

    long countByOccurredAtBetween(Instant from, Instant to);

    /** Just the timestamps in a window, for building the event-volume histogram. */
    @Query("select e.occurredAt from EventEntity e where e.occurredAt between ?1 and ?2")
    List<Instant> findOccurredAtBetween(Instant from, Instant to);
}
