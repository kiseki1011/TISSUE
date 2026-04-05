package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.security.adapter.web.request.LoginRequest;
import com.tissue.security.adapter.web.request.SignupMemberRequest;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Test
    @DisplayName("signup -> login -> access protected endpoint")
    void signupThenLoginThenAccessProtectedEndpoint() throws Exception {
        String email = "test@trytissue.dev";
        String verifiedToken = simulateEmailVerification(email);

        // signup
        var signupRequest = new SignupMemberRequest(email, "testuser", "password1234!", "TestUser", verifiedToken);

        mockMvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // login with account
        var loginRequest = new LoginRequest(email, "password1234!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse =
                objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // access endpoint with token
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + loginResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("401: access protected endpoint without token")
    void accessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401: access protected endpoint with invalid token")
    void accessProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    private String simulateEmailVerification(String email) {
        String emailToken = UUID.randomUUID().toString();
        String verificationId = UUID.randomUUID().toString();

        emailVerificationRepository.storeVerificationContext(
                verificationId, email, emailToken, emailVerificationProperties.getEmailTtl());
        emailVerificationRepository.verifyByEmailToken(emailToken, emailVerificationProperties.getVerifiedTokenTtl());

        VerificationStatus status = emailVerificationRepository.getStatus(verificationId);
        return status.verifiedToken();
    }
}
