package com.pulseops.events.api;

import com.pulseops.common.PageResponse;
import com.pulseops.common.error.ValidationException;
import com.pulseops.events.EventIngestionService;
import com.pulseops.events.EventQuery;
import com.pulseops.events.EventQueryService;
import com.pulseops.events.dto.AcceptedEventResponse;
import com.pulseops.events.dto.EventRequest;
import com.pulseops.events.dto.EventResponse;
import com.pulseops.events.model.EventType;
import com.pulseops.events.model.Severity;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Event ingestion and querying.
 *
 * <ul>
 *   <li>{@code POST /api/events} — 202 Accepted; the event is stored and queued
 *       for correlation.</li>
 *   <li>{@code GET /api/events} — filter by service / severity / type / time
 *       range, paginated.</li>
 *   <li>{@code GET /api/events/{id}} — one event by {@code EVT-<n>}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final int MAX_PAGE_SIZE = 500;

    private final EventIngestionService ingestionService;
    private final EventQueryService queryService;

    public EventController(EventIngestionService ingestionService, EventQueryService queryService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AcceptedEventResponse ingest(@Valid @RequestBody EventRequest request) {
        return ingestionService.ingest(request);
    }

    @GetMapping
    public PageResponse<EventResponse> list(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        EventQuery query = new EventQuery(
                service,
                parseEnum(Severity.class, severity, "severity"),
                parseEnum(EventType.class, eventType, "eventType"),
                from,
                to);

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "occurredAt"));

        return PageResponse.from(queryService.search(query, pageable));
    }

    @GetMapping("/{id}")
    public EventResponse byId(@PathVariable String id) {
        return queryService.getByPublicId(id);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("unknown " + field + " filter '" + raw + "'");
        }
    }
}
