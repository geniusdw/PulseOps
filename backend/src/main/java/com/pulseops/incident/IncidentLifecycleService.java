package com.pulseops.incident;

import com.pulseops.common.error.ConflictException;
import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.model.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Enforces the incident state machine:
 * <pre>OPEN → INVESTIGATING → RESOLVED  (and OPEN → RESOLVED directly)</pre>
 * Illegal transitions (e.g. resolving an already-resolved incident) raise 409.
 */
@Service
public class IncidentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentQueryService queryService;
    private final Clock clock;

    public IncidentLifecycleService(IncidentRepository incidentRepository,
                                    IncidentQueryService queryService,
                                    Clock clock) {
        this.incidentRepository = incidentRepository;
        this.queryService = queryService;
        this.clock = clock;
    }

    @Transactional
    public IncidentEntity acknowledge(String publicId) {
        IncidentEntity incident = queryService.require(publicId);
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ConflictException("INVALID_TRANSITION",
                    incident.getPublicId() + " is RESOLVED and cannot be acknowledged");
        }
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
            incident.setUpdatedAt(clock.instant());
            log.info("Incident {} acknowledged -> INVESTIGATING", incident.getPublicId());
        }
        return incidentRepository.save(incident);
    }

    @Transactional
    public IncidentEntity resolve(String publicId) {
        IncidentEntity incident = queryService.require(publicId);
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ConflictException("INVALID_TRANSITION",
                    incident.getPublicId() + " is already RESOLVED");
        }
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(clock.instant());
        incident.setUpdatedAt(clock.instant());
        log.info("Incident {} resolved", incident.getPublicId());
        return incidentRepository.save(incident);
    }
}
