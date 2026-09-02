package com.pulseops.support;

import com.pulseops.topology.ServiceDependencyRepository;
import com.pulseops.topology.ServiceNodeRepository;
import com.pulseops.topology.TopologyService;
import com.pulseops.topology.model.ServiceDependency;
import com.pulseops.topology.model.ServiceNode;
import com.pulseops.topology.model.ServiceNode.Tier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a {@link TopologyService} backed by the default PulseOps topology, for
 * unit tests that need dependency-distance logic without a Spring context.
 */
public final class TestTopology {

    private TestTopology() {
    }

    public static TopologyService standard() {
        ServiceNodeRepository nodes = mock(ServiceNodeRepository.class);
        ServiceDependencyRepository deps = mock(ServiceDependencyRepository.class);

        List<ServiceNode> nodeList = List.of(
                new ServiceNode("api-gateway", "API Gateway", Tier.EDGE, ""),
                new ServiceNode("payment-api", "Payment API", Tier.APPLICATION, ""),
                new ServiceNode("transaction-service", "Transaction Service", Tier.APPLICATION, ""),
                new ServiceNode("user-service", "User Service", Tier.APPLICATION, ""),
                new ServiceNode("notification-service", "Notification Service", Tier.APPLICATION, ""),
                new ServiceNode("database", "Database", Tier.DATA, "")
        );
        when(nodes.findAll()).thenReturn(nodeList);
        when(nodes.findById(anyString())).thenAnswer(inv ->
                nodeList.stream().filter(n -> n.getName().equals(inv.getArgument(0))).findFirst());
        when(nodes.count()).thenReturn((long) nodeList.size());
        when(deps.findAll()).thenReturn(List.of(
                new ServiceDependency("api-gateway", "payment-api"),
                new ServiceDependency("api-gateway", "user-service"),
                new ServiceDependency("api-gateway", "notification-service"),
                new ServiceDependency("payment-api", "transaction-service"),
                new ServiceDependency("payment-api", "notification-service"),
                new ServiceDependency("transaction-service", "database"),
                new ServiceDependency("user-service", "database")
        ));

        TopologyService topology = new TopologyService(nodes, deps);
        topology.invalidate();
        return topology;
    }
}
