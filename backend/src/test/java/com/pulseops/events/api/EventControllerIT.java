package com.pulseops.events.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseops.events.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void validEventIsAccepted() throws Exception {
        EventRequest request = new EventRequest(Instant.now(), "payment-api", "node-1",
                "HTTP_500", "HIGH", "error_rate", 18.4, "500 errors increased", null);

        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId", containsString("EVT-")))
                .andExpect(jsonPath("$.status").value("QUEUED_FOR_CORRELATION"));
    }

    @Test
    void missingRequiredFieldReturns400WithErrorBody() throws Exception {
        String body = """
                { "service": "", "eventType": "HTTP_500", "severity": "HIGH", "message": "" }
                """;

        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void unknownServiceReturns400() throws Exception {
        EventRequest request = new EventRequest(Instant.now(), "not-a-real-service", null,
                "HTTP_500", "HIGH", null, null, "boom", null);

        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("validation")));
    }

    @Test
    void unknownEventTypeReturns400() throws Exception {
        EventRequest request = new EventRequest(Instant.now(), "payment-api", null,
                "HTTP_418", "HIGH", null, null, "teapot", null);

        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEventReturns404() throws Exception {
        mvc.perform(get("/api/events/EVT-99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVENT_NOT_FOUND"));
    }

    @Test
    void malformedIdReturns400() throws Exception {
        mvc.perform(get("/api/events/not-an-id"))
                .andExpect(status().isBadRequest());
    }
}
