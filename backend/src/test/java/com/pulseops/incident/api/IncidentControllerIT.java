package com.pulseops.incident.api;

import com.pulseops.simulator.Scenario;
import com.pulseops.simulator.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    SimulatorService simulator;

    private String incidentId;

    @BeforeEach
    void seedIncident() {
        incidentId = simulator.run(Scenario.CPU_SATURATION).eventIds().isEmpty()
                ? null : firstIncidentId();
    }

    private String firstIncidentId() throws AssertionError {
        try {
            String body = mvc.perform(get("/api/incidents?size=1"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            int idx = body.indexOf("\"incidentId\":\"");
            return body.substring(idx + 14, body.indexOf('"', idx + 14));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void listReturnsIncidents() throws Exception {
        mvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void detailReturnsCorrelationAndRootCause() throws Exception {
        mvc.perform(get("/api/incidents/" + incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.rootCauses").isArray())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void rootCauseEndpointCarriesDisclaimer() throws Exception {
        mvc.perform(get("/api/incidents/" + incidentId + "/root-cause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disclaimer").exists())
                .andExpect(jsonPath("$.candidates").isArray());
    }

    @Test
    void acknowledgeThenResolveFollowsStateMachine() throws Exception {
        mvc.perform(post("/api/incidents/" + incidentId + "/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));

        mvc.perform(post("/api/incidents/" + incidentId + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // resolving again is a conflict
        mvc.perform(post("/api/incidents/" + incidentId + "/resolve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        mvc.perform(get("/api/incidents/INC-99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void explainReturnsValidatedStructuredExplanation() throws Exception {
        mvc.perform(post("/api/incidents/" + incidentId + "/explain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.probableCause").isNotEmpty())
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.recommendedChecks").isArray());
    }
}
