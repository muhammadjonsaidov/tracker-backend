package com.rhaen.tracker.feature.tracking.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhaen.tracker.feature.auth.dto.AuthDtos;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrackingControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void start_ingest_stop_flow_success() throws Exception {
        String token = registerAndLogin("trk1");

        String startBody = mockMvc.perform(post("/api/v1/tracking/sessions/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID sessionId = UUID.fromString(objectMapper.readTree(startBody).path("data").path("sessionId").asText());

        var p = new TrackingDtos.LocationPoint(
                UUID.randomUUID(),
                41.3111,
                69.2797,
                Instant.now(),
                3f,
                1.5f,
                120f,
                "gps",
                false
        );
        var ingestReq = new TrackingDtos.IngestPointsRequest(List.of(p));

        mockMvc.perform(post("/api/v1/tracking/sessions/" + sessionId + "/points")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tracking/sessions/" + sessionId + "/stop")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrackingDtos.StopSessionRequest(null, null, null))))
                .andExpect(status().isOk());
    }

    @Test
    void mySessions_isPaginated() throws Exception {
        String token = registerAndLogin("trk2");
        String body = mockMvc.perform(get("/api/v1/tracking/sessions?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        assertThat(data.has("content")).isTrue();
        assertThat(data.has("size")).isTrue();
    }

    @Test
    void user_cannot_access_other_user_session_points() throws Exception {
        String token1 = registerAndLogin("trk3a");
        String token2 = registerAndLogin("trk3b");

        String startBody = mockMvc.perform(post("/api/v1/tracking/sessions/start")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID sessionId = UUID.fromString(objectMapper.readTree(startBody).path("data").path("sessionId").asText());

        mockMvc.perform(get("/api/v1/tracking/sessions/" + sessionId + "/points")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "_" + suffix;
        String email = prefix + "_" + suffix + "@mail.com";
        String password = "pass12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDtos.RegisterRequest(username, email, password))))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(loginBody).path("data").path("accessToken").asText();
    }
}
