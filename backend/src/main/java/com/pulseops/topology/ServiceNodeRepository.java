package com.pulseops.topology;

import com.pulseops.topology.model.ServiceNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceNodeRepository extends JpaRepository<ServiceNode, String> {
}
