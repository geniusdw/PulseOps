package com.pulseops.incident.api;

import com.pulseops.common.PageResponse;
import com.pulseops.common.error.ValidationException;
import com.pulseops.events.dto.EventResponse;
import com.pulseops.events.model.Severity;
import com.pulseops.incident.IncidentLifecycleService;
import com.pulseops.incident.IncidentQueryService;
import com.pulseops.incident.model.IncidentStatus;
import com.pulseops.incident.timeline.TimelineEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentQueryService queryService;
    private final IncidentLifecycleService lifecycleService;

    public IncidentController(IncidentQueryService queryService,
                              IncidentLifecycleService lifecycleService) {
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @GetMapping
    public PageResponse<IncidentSummaryDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return queryService.list(
                parseEnum(IncidentStatus.class, status, "status"),
                parseEnum(Severity.class, severity, "severity"),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                        Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    @GetMapping("/{id}")
    public IncidentDetailDto detail(@PathVariable String id) {
        return queryService.detail(id);
    }

    @GetMapping("/{id}/events")
    public List<EventResponse> events(@PathVariable String id) {
        return queryService.eventsFor(queryService.require(id).getId());
    }

    @GetMapping("/{id}/timeline")
    public List<TimelineEntry> timeline(@PathVariable String id) {
        return queryService.timeline(id);
    }

    @GetMapping("/{id}/root-cause")
    public RootCauseResponse rootCause(@PathVariable String id) {
        return queryService.rootCause(id);
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentDetailDto acknowledge(@PathVariable String id) {
        lifecycleService.acknowledge(id);
        return queryService.detail(id);
    }

    @PostMapping("/{id}/resolve")
    public IncidentDetailDto resolve(@PathVariable String id) {
        lifecycleService.resolve(id);
        return queryService.detail(id);
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
