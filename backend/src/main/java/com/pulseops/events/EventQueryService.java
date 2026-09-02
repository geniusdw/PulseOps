package com.pulseops.events;

import com.pulseops.common.PublicIds;
import com.pulseops.common.error.ResourceNotFoundException;
import com.pulseops.events.dto.EventResponse;
import com.pulseops.events.model.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side service for events. */
@Service
@Transactional(readOnly = true)
public class EventQueryService {

    private final EventRepository eventRepository;

    public EventQueryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Page<EventResponse> search(EventQuery query, Pageable pageable) {
        return eventRepository.findAll(query.toSpecification(), pageable).map(EventResponse::from);
    }

    public EventResponse getByPublicId(String publicId) {
        long id = PublicIds.parse("EVT", publicId);
        EventEntity event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EVENT_NOT_FOUND", "Event EVT-" + id + " was not found"));
        return EventResponse.from(event);
    }
}
