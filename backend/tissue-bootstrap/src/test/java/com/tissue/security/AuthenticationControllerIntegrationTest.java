package com.tissue.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.adapter.web.request.LoginRequest;
import com.tissue.security.adapter.web.request.RefreshTokenRequest;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.TokenProvider;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    private Member member;

    @BeforeEach
    void setUpMember() {
        member = Member.create("test@trytissue.dev", "testuser", "TestUser");
        memberCommandRepository.save(member);

        AuthenticationIdentity identity = AuthenticationIdentity.createEmailIdentity(
                member, "test@trytissue.dev", passwordEncoder.encode("password1234!"));
        authenticationIdentityRepository.save(identity);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("200: login with valid credentials")
        void loginSuccess() throws Exception {
            var request = new LoginRequest("test@trytissue.dev", "password1234!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("401: login with wrong password")
        void loginWithWrongPassword() throws Exception {
            var request = new LoginRequest("test@trytissue.dev", "wrongpassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400: login with blank credentials")
        void loginWithEmptyRequest() throws Exception {
            var request = new LoginRequest("", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/token:refresh")
    class RefreshToken {

        @Test
        @DisplayName("200: refresh with valid token")
        void refreshTokenSuccess() throws Exception {
            var loginRequest = new LoginRequest("test@trytissue.dev", "password1234!");

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            LoginResponse loginResponse =
                    objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

            String refreshToken = loginResponse.refreshToken();

            var refreshRequest = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post("/api/v1/auth/token:refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("401: refresh with invalid token")
        void refreshTokenWithInvalidToken() throws Exception {
            var request = new RefreshTokenRequest("invalid.token.value");

            mockMvc.perform(post("/api/v1/auth/token:refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("204: logout with authenticated user")
        void logoutSuccess() throws Exception {
            String accessToken = createAccessToken();

            mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("401: logout without authentication")
        void logoutWithoutAuthentication() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
        }
    }

    private String createAccessToken() {
        return tokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())));
    }
}
