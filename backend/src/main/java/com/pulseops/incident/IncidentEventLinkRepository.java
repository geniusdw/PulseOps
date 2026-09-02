package com.pulseops.incident;

import com.pulseops.incident.model.IncidentEventLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface IncidentEventLinkRepository extends JpaRepository<IncidentEventLink, Long> {

    List<IncidentEventLink> findByIncidentId(Long incidentId);

    List<IncidentEventLink> findByIncidentIdIn(Collection<Long> incidentIds);

    List<IncidentEventLink> findByEventIdIn(Collection<Long> eventIds);

    boolean existsByIncidentIdAndEventId(Long incidentId, Long eventId);

    @Transactional
    void deleteByIncidentId(Long incidentId);
}
