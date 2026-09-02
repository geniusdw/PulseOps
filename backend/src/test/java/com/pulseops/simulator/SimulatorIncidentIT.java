package com.pulseops.simulator;

import com.pulseops.incident.IncidentQueryService;
import com.pulseops.incident.IncidentRepository;
import com.pulseops.incident.api.IncidentDetailDto;
import com.pulseops.incident.api.IncidentSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: inject a scenario through the real ingest + correlation pipeline
 * and assert the incident that comes out.
 */
@SpringBootTest
@ActiveProfiles("test")
class SimulatorIncidentIT {

    @Autowired
    SimulatorService simulator;
    @Autowired
    IncidentRepository incidentRepository;
    @Autowired
    IncidentQueryService incidentQueryService;

    @Test
    void databaseFailureScenarioProducesOneCorrelatedIncident() {
        long before = incidentRepository.count();

        ScenarioRunResult result = simulator.run(Scenario.DATABASE_FAILURE);

        assertThat(result.eventsCreated()).isEqualTo(6);
        assertThat(incidentRepository.count()).isGreaterThan(before);

        IncidentSummaryDto summary = incidentQueryService
                .list(null, null, org.springframework.data.domain.PageRequest.of(0, 1,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "updatedAt")))
                .content().get(0);

        IncidentDetailDto detail = incidentQueryService.detail(summary.incidentId());
        assertThat(detail.affectedServices()).contains("database", "payment-api");
        assertThat(detail.probableRootCause()).containsIgnoringCase("database");
        assertThat(detail.events()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(detail.timeline()).isNotEmpty();
        assertThat(detail.correlation()).isNotNull();
    }

    @Test
    void normalTrafficDoesNotCreateAnIncident() {
        long before = incidentRepository.count();

        simulator.run(Scenario.NORMAL_TRAFFIC);

        assertThat(incidentRepository.count()).isEqualTo(before);
    }
}
