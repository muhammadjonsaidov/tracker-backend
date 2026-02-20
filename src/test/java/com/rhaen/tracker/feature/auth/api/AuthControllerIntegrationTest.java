package com.rhaen.tracker.feature.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhaen.tracker.BaseIntegrationTest;
import com.rhaen.tracker.feature.auth.dto.AuthDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseIntegrationTest {
        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void register_and_login_success() throws Exception {
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                String username = "it_user_" + suffix;
                String email = "it_" + suffix + "@mail.com";
                String password = "pass12345";

                var registerReq = new AuthDtos.RegisterRequest(username, email, password);
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerReq)))
                                .andExpect(status().isOk());

                var loginReq = new AuthDtos.LoginRequest(username, password);
                String body = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode root = objectMapper.readTree(body);
                assertThat(root.path("data").path("accessToken").asText()).isNotBlank();
                assertThat(root.path("data").path("tokenType").asText()).isEqualTo("Bearer");
        }

        @Test
        void register_validationError_returns400() throws Exception {
                String body = """
                                {"username":"", "email":"invalid-email", "password":""}
                                """;
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest());
        }
}
