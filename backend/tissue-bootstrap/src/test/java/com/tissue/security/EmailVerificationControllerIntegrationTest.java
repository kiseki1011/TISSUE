package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tissue.security.adapter.web.request.EmailVerificationRequest;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
class EmailVerificationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Nested
    @DisplayName("POST /api/v1/members/signup:requestVerification")
    class RequestVerification {

        @Test
        @DisplayName("200: request verification with valid email")
        void requestVerificationSuccess() throws Exception {
            var request = new EmailVerificationRequest("test@trytissue.dev");

            mockMvc.perform(post("/api/v1/members/signup:requestVerification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verificationId").isNotEmpty());
        }

        @Test
        @DisplayName("400: request verification with invalid email format")
        void requestVerificationWithInvalidEmail() throws Exception {
            var request = new EmailVerificationRequest("bad-email");

            mockMvc.perform(post("/api/v1/members/signup:requestVerification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/members/signup/verify")
    class VerifyEmail {

        @Test
        @DisplayName("200: verify email with valid token")
        void verifyEmailSuccess() throws Exception {
            String emailToken = UUID.randomUUID().toString();
            String verificationId = UUID.randomUUID().toString();

            emailVerificationRepository.storeVerificationContext(
                    verificationId, "test@trytissue.dev", emailToken, emailVerificationProperties.getEmailTtl());

            mockMvc.perform(get("/api/v1/members/signup/verify").param("token", emailToken))
                    .andExpect(status().isOk())
                    .andExpect(view().name("verification-success"));
        }

        @Test
        @DisplayName("200: verify email with invalid token returns failure view")
        void verifyEmailWithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/members/signup/verify").param("token", "invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("verification-failure"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/members/signup/status/{verificationId}")
    class CheckVerificationStatus {

        @Test
        @DisplayName("200: status is PENDING before verification")
        void statusPending() throws Exception {
            String emailToken = UUID.randomUUID().toString();
            String verificationId = UUID.randomUUID().toString();

            emailVerificationRepository.storeVerificationContext(
                    verificationId, "test@trytissue.dev", emailToken, emailVerificationProperties.getEmailTtl());

            mockMvc.perform(get("/api/v1/members/signup/status/{verificationId}", verificationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.verifiedToken").isEmpty());
        }

        @Test
        @DisplayName("200: status is VERIFIED after email verification")
        void statusVerified() throws Exception {
            String emailToken = UUID.randomUUID().toString();
            String verificationId = UUID.randomUUID().toString();

            emailVerificationRepository.storeVerificationContext(
                    verificationId, "test@trytissue.dev", emailToken, emailVerificationProperties.getEmailTtl());
            emailVerificationRepository.verifyByEmailToken(
                    emailToken, emailVerificationProperties.getVerifiedTokenTtl());

            mockMvc.perform(get("/api/v1/members/signup/status/{verificationId}", verificationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VERIFIED"))
                    .andExpect(jsonPath("$.verifiedToken").isNotEmpty());
        }

        @Test
        @DisplayName("200: status is UNKNOWN for wrong verificationId")
        void statusUnknown() throws Exception {
            mockMvc.perform(get("/api/v1/members/signup/status/{verificationId}", "invalid-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNKNOWN"));
        }
    }
}
