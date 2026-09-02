package com.pulseops.deployments;

import com.pulseops.deployments.model.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeploymentRepository extends JpaRepository<DeploymentEntity, Long> {

    List<DeploymentEntity> findByDeployedAtBetweenOrderByDeployedAtAsc(Instant from, Instant to);

    List<DeploymentEntity> findByServiceAndDeployedAtBetween(String service, Instant from, Instant to);
}
