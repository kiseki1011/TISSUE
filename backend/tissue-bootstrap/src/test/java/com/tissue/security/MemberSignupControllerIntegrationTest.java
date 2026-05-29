package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.security.adapter.web.request.SignupMemberRequest;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class MemberSignupControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TissueSecurityProperties tissueSecurityProperties;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @AfterEach
    void tearDown() {
        tissueSecurityProperties.setEmailRequired(true);
    }

    @Nested
    @DisplayName("POST /api/v1/members/signup - email-required=true")
    class EmailSignup {

        @Test
        @DisplayName("201: signup with verified email")
        void signupWithVerifiedEmail() throws Exception {
            String email = "test@trytissue.dev";
            String verifiedToken = simulateEmailVerification(email);

            var request = new SignupMemberRequest(email, "testuser", "password1234!", "TestUser", verifiedToken);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.memberId").isNumber());
        }

        @Test
        @DisplayName("403: signup with unverified email token")
        void signupWithInvalidVerifiedToken() throws Exception {
            var request = new SignupMemberRequest(
                    "test@trytissue.dev", "testuser", "password1234!", "TestUser", "invalid-token");

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("409: signup with duplicate email")
        void signupWithDuplicateEmail() throws Exception {
            String email = "duplicate@trytissue.dev";
            String verifiedToken1 = simulateEmailVerification(email);

            var first = new SignupMemberRequest(email, "firstuser", "password1234!", "FirstUser", verifiedToken1);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(first)))
                    .andExpect(status().isCreated());

            String verifiedToken2 = simulateEmailVerification(email);

            var second = new SignupMemberRequest(email, "seconduser", "password1234!", "SecondUser", verifiedToken2);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(second)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("409: signup with duplicate username")
        void signupWithDuplicateUsername() throws Exception {
            String verifiedToken1 = simulateEmailVerification("first@trytissue.dev");

            var first = new SignupMemberRequest(
                    "first@trytissue.dev", "sameuser", "password1234!", "FirstUser", verifiedToken1);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(first)))
                    .andExpect(status().isCreated());

            String verifiedToken2 = simulateEmailVerification("second@trytissue.dev");

            var second = new SignupMemberRequest(
                    "second@trytissue.dev", "sameuser", "password1234!", "SecondUser", verifiedToken2);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(second)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("400: signup with blank username")
        void signupWithInvalidRequest() throws Exception {
            var request = new SignupMemberRequest("test@trytissue.dev", "", "password1234!", "TestUser", "some-token");

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/members/signup - email-required=false")
    class UsernameSignup {

        @Test
        @DisplayName("201: signup with username only")
        void signupWithUsernameOnly() throws Exception {
            tissueSecurityProperties.setEmailRequired(false);

            var request = new SignupMemberRequest(null, "testuser", "password1234!", "TestUser", null);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.memberId").isNumber());
        }

        @Test
        @DisplayName("409: signup with duplicate username")
        void signupWithDuplicateUsername() throws Exception {
            tissueSecurityProperties.setEmailRequired(false);

            var first = new SignupMemberRequest(null, "sameuser", "password1234!", "FirstUser", null);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(first)))
                    .andExpect(status().isCreated());

            var second = new SignupMemberRequest(null, "sameuser", "password1234!", "SecondUser", null);

            mockMvc.perform(post("/api/v1/members/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(second)))
                    .andExpect(status().isConflict());
        }
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
