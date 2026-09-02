package com.pulseops.events;

import com.pulseops.deployments.DeploymentRepository;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The transactional write path for events. Kept separate from
 * {@link EventIngestionService} so that enqueuing onto the correlation queue
 * happens <em>after</em> the DB commit — a full queue then returns 429 without
 * rolling back an already-stored event (the scheduled sweep will still correlate
 * it).
 */
@Service
public class EventPersistenceService {

    private final EventRepository eventRepository;
    private final DeploymentRepository deploymentRepository;
    private final Clock clock;

    public EventPersistenceService(EventRepository eventRepository,
                                   DeploymentRepository deploymentRepository,
                                   Clock clock) {
        this.eventRepository = eventRepository;
        this.deploymentRepository = deploymentRepository;
        this.clock = clock;
    }

    /**
     * Persist one event. If it is a {@code DEPLOYMENT}, also record a
     * {@link DeploymentEntity} and link the event to it. Otherwise, if
     * {@code deploymentIdOverride} is supplied (used by the simulator to model a
     * bad-deployment blast radius), link to that deployment.
     */
    @Transactional
    public EventEntity persist(NormalizedEvent n, Long deploymentIdOverride) {
        Long deploymentId = deploymentIdOverride;

        if (n.eventType() == EventType.DEPLOYMENT) {
            DeploymentEntity deployment = deploymentRepository.save(
                    new DeploymentEntity(n.service(), n.deploymentVersion(), n.occurredAt()));
            deploymentId = deployment.getId();
        }

        EventEntity event = new EventEntity(
                n.occurredAt(),
                clock.instant(),
                n.service(),
                n.host(),
                n.eventType(),
                n.severity(),
                n.metric(),
                n.value(),
                n.message(),
                deploymentId);

        return eventRepository.save(event);
    }
}
