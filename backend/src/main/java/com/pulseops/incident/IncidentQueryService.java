package com.pulseops.incident;

import com.pulseops.common.PageResponse;
import com.pulseops.common.PublicIds;
import com.pulseops.common.error.ResourceNotFoundException;
import com.pulseops.correlation.CorrelationEngine;
import com.pulseops.correlation.model.CorrelationExplanation;
import com.pulseops.correlation.model.EventCluster;
import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.events.EventRepository;
import com.pulseops.events.dto.EventResponse;
import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.Severity;
import com.pulseops.incident.api.IncidentDetailDto;
import com.pulseops.incident.api.IncidentSummaryDto;
import com.pulseops.incident.api.RootCauseDto;
import com.pulseops.incident.api.RootCauseResponse;
import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.model.IncidentEventLink;
import com.pulseops.incident.model.IncidentStatus;
import com.pulseops.incident.rootcause.RootCauseRanker;
import com.pulseops.incident.timeline.TimelineBuilder;
import com.pulseops.incident.timeline.TimelineEntry;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-side service for incidents: list, detail, events, timeline, root cause. */
@Service
@Transactional(readOnly = true)
public class IncidentQueryService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventLinkRepository linkRepository;
    private final EventRepository eventRepository;
    private final CorrelationEngine engine;
    private final RootCauseRanker rootCauseRanker;
    private final TimelineBuilder timelineBuilder;
    private final IncidentManager incidentManager;

    public IncidentQueryService(IncidentRepository incidentRepository,
                                IncidentEventLinkRepository linkRepository,
                                EventRepository eventRepository,
                                CorrelationEngine engine,
                                RootCauseRanker rootCauseRanker,
                                TimelineBuilder timelineBuilder,
                                IncidentManager incidentManager) {
        this.incidentRepository = incidentRepository;
        this.linkRepository = linkRepository;
        this.eventRepository = eventRepository;
        this.engine = engine;
        this.rootCauseRanker = rootCauseRanker;
        this.timelineBuilder = timelineBuilder;
        this.incidentManager = incidentManager;
    }

    public PageResponse<IncidentSummaryDto> list(IncidentStatus status, Severity severity, Pageable pageable) {
        Specification<IncidentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<IncidentSummaryDto> page = incidentRepository.findAll(spec, pageable)
                .map(this::toSummary);
        return PageResponse.from(page);
    }

    public IncidentEntity require(String publicId) {
        long id = PublicIds.parse("INC", publicId);
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "INCIDENT_NOT_FOUND", "Incident INC-" + id + " was not found"));
    }

    public IncidentDetailDto detail(String publicId) {
        IncidentEntity incident = require(publicId);
        List<SignalEvent> events = signalEventsFor(incident.getId());
        List<String> affected = affectedServices(events);

        List<EventCluster> clusters = engine.correlate(events);
        CorrelationExplanation correlation = clusters.isEmpty() ? null : clusters.get(0).explanation();

        List<DeploymentEntity> deployments = events.isEmpty()
                ? List.of()
                : incidentManager.relevantDeployments(events);
        List<RootCauseDto> rootCauses = rootCauseRanker.rank(events, deployments).stream()
                .map(RootCauseDto::from)
                .toList();

        List<TimelineEntry> timeline = timelineBuilder.build(incident, events, deployments);
        List<EventResponse> eventResponses = eventsFor(incident.getId());

        return IncidentDetailDto.of(incident, affected, correlation, rootCauses, timeline, eventResponses);
    }

    public List<EventResponse> eventsFor(Long incidentId) {
        List<Long> eventIds = linkRepository.findByIncidentId(incidentId).stream()
                .map(IncidentEventLink::getEventId)
                .toList();
        return eventRepository.findAllById(eventIds).stream()
                .sorted(Comparator.comparing(EventEntity::getOccurredAt))
                .map(EventResponse::from)
                .toList();
    }

    public List<TimelineEntry> timeline(String publicId) {
        IncidentEntity incident = require(publicId);
        List<SignalEvent> events = signalEventsFor(incident.getId());
        List<DeploymentEntity> deployments = events.isEmpty()
                ? List.of() : incidentManager.relevantDeployments(events);
        return timelineBuilder.build(incident, events, deployments);
    }

    public RootCauseResponse rootCause(String publicId) {
        IncidentEntity incident = require(publicId);
        List<SignalEvent> events = signalEventsFor(incident.getId());
        List<DeploymentEntity> deployments = events.isEmpty()
                ? List.of() : incidentManager.relevantDeployments(events);
        List<RootCauseDto> candidates = rootCauseRanker.rank(events, deployments).stream()
                .map(RootCauseDto::from)
                .toList();
        return new RootCauseResponse(incident.getPublicId(), candidates, RootCauseResponse.DISCLAIMER);
    }

    public List<SignalEvent> signalEventsFor(Long incidentId) {
        List<Long> eventIds = linkRepository.findByIncidentId(incidentId).stream()
                .map(IncidentEventLink::getEventId)
                .toList();
        List<SignalEvent> events = new ArrayList<>(
                eventRepository.findAllById(eventIds).stream().map(SignalEvent::from).toList());
        events.sort(Comparator.comparing(SignalEvent::occurredAt));
        return events;
    }

    private IncidentSummaryDto toSummary(IncidentEntity incident) {
        List<IncidentEventLink> links = linkRepository.findByIncidentId(incident.getId());
        List<Long> eventIds = links.stream().map(IncidentEventLink::getEventId).toList();
        List<String> affected = eventRepository.findAllById(eventIds).stream()
                .map(EventEntity::getService)
                .distinct()
                .sorted()
                .toList();
        return IncidentSummaryDto.of(incident, affected, links.size());
    }

    private static List<String> affectedServices(List<SignalEvent> events) {
        return events.stream().map(SignalEvent::service).distinct().sorted().toList();
    }
}
