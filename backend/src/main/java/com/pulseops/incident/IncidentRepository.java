package com.pulseops.incident;

import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long>,
        JpaSpecificationExecutor<IncidentEntity> {

    List<IncidentEntity> findByStatusNot(IncidentStatus status);

    List<IncidentEntity> findByStatusInOrderByStartedAtDesc(List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countByStatusNot(IncidentStatus status);
}
