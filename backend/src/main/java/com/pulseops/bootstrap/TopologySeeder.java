package com.pulseops.bootstrap;

import com.pulseops.topology.ServiceDependencyRepository;
import com.pulseops.topology.ServiceNodeRepository;
import com.pulseops.topology.TopologyService;
import com.pulseops.topology.model.ServiceDependency;
import com.pulseops.topology.model.ServiceNode;
import com.pulseops.topology.model.ServiceNode.Tier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the default service topology on first start (when the {@code services}
 * table is empty). In production this would be a migration or a topology
 * imported from a service catalogue / CMDB; for a self-contained demo a seeded
 * default is the pragmatic choice.
 */
@Component
@Order(1)
public class TopologySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TopologySeeder.class);

    private final ServiceNodeRepository nodes;
    private final ServiceDependencyRepository dependencies;
    private final TopologyService topology;

    public TopologySeeder(ServiceNodeRepository nodes,
                          ServiceDependencyRepository dependencies,
                          TopologyService topology) {
        this.nodes = nodes;
        this.dependencies = dependencies;
        this.topology = topology;
    }

    @Override
    public void run(String... args) {
        if (nodes.count() > 0) {
            topology.invalidate();
            return;
        }
        log.info("Seeding default service topology");

        nodes.saveAll(List.of(
                new ServiceNode("api-gateway", "API Gateway", Tier.EDGE,
                        "Public entry point; routes external traffic to services"),
                new ServiceNode("payment-api", "Payment API", Tier.APPLICATION,
                        "Accepts and orchestrates payment requests"),
                new ServiceNode("transaction-service", "Transaction Service", Tier.APPLICATION,
                        "Records and settles transactions"),
                new ServiceNode("user-service", "User Service", Tier.APPLICATION,
                        "User profiles and authentication data"),
                new ServiceNode("notification-service", "Notification Service", Tier.APPLICATION,
                        "Sends email / push notifications"),
                new ServiceNode("database", "Database", Tier.DATA,
                        "Primary relational datastore shared by application services")
        ));

        dependencies.saveAll(List.of(
                new ServiceDependency("api-gateway", "payment-api"),
                new ServiceDependency("api-gateway", "user-service"),
                new ServiceDependency("api-gateway", "notification-service"),
                new ServiceDependency("payment-api", "transaction-service"),
                new ServiceDependency("payment-api", "notification-service"),
                new ServiceDependency("transaction-service", "database"),
                new ServiceDependency("user-service", "database")
        ));

        topology.invalidate();
        log.info("Seeded {} services and {} dependency edges", nodes.count(), dependencies.count());
    }
}
