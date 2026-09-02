package com.pulseops.topology;

import com.pulseops.topology.model.ServiceDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceDependencyRepository extends JpaRepository<ServiceDependency, Long> {
}
