package com.pulseops.topology;

import com.pulseops.support.TestTopology;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyServiceTest {

    private final TopologyService topology = TestTopology.standard();

    @Test
    void sameServiceHasZeroDistance() {
        assertThat(topology.dependencyDistance("payment-api", "payment-api")).isZero();
    }

    @Test
    void directDependencyIsOneHop() {
        assertThat(topology.dependencyDistance("payment-api", "transaction-service")).isEqualTo(1);
        assertThat(topology.dependencyDistance("transaction-service", "database")).isEqualTo(1);
    }

    @Test
    void transitiveDependencyDistanceIsShortestPath() {
        // payment-api -> transaction-service -> database
        assertThat(topology.dependencyDistance("payment-api", "database")).isEqualTo(2);
        // api-gateway -> user-service -> database  (shortest of several paths)
        assertThat(topology.dependencyDistance("api-gateway", "database")).isEqualTo(2);
        // notification-service -> payment-api -> transaction-service -> database
        assertThat(topology.dependencyDistance("notification-service", "database")).isEqualTo(3);
    }

    @Test
    void unknownServiceReturnsMinusOne() {
        assertThat(topology.dependencyDistance("payment-api", "does-not-exist")).isEqualTo(-1);
    }

    @Test
    void dependentsAndDependenciesAreDirectional() {
        assertThat(topology.directDependenciesOf("payment-api"))
                .containsExactlyInAnyOrder("transaction-service", "notification-service");
        assertThat(topology.directDependentsOf("database"))
                .containsExactlyInAnyOrder("transaction-service", "user-service");
    }
}
