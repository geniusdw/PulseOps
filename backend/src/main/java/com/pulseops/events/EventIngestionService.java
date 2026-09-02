package com.pulseops.events;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.events.dto.AcceptedEventResponse;
import com.pulseops.events.dto.EventRequest;
import com.pulseops.events.model.EventEntity;
import com.pulseops.ingest.IngestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a single ingest: validate/normalise -> persist (committed) ->
 * enqueue for asynchronous correlation.
 */
@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final EventValidator validator;
    private final EventPersistenceService persistence;
    private final IngestQueue queue;

    public EventIngestionService(EventValidator validator,
                                 EventPersistenceService persistence,
                                 IngestQueue queue) {
        this.validator = validator;
        this.persistence = persistence;
        this.queue = queue;
    }

    /** Public API entry point for {@code POST /api/events}. */
    public AcceptedEventResponse ingest(EventRequest request) {
        NormalizedEvent normalized = validator.validate(request);
        EventEntity saved = persistence.persist(normalized, null);
        enqueue(saved);
        log.debug("Ingested {} ({} on {})", saved.getPublicId(), saved.getEventType(), saved.getService());
        return new AcceptedEventResponse(saved.getPublicId(), "QUEUED_FOR_CORRELATION", queue.size());
    }

    /** Entry point for the simulator: already-normalised event, optional deployment link. */
    public EventEntity ingestNormalized(NormalizedEvent normalized, Long deploymentId) {
        EventEntity saved = persistence.persist(normalized, deploymentId);
        enqueue(saved);
        return saved;
    }

    private void enqueue(EventEntity saved) {
        queue.submit(SignalEvent.from(saved));
    }
}
