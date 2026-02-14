package com.tissue.security.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.application.dto.response.LoginResponse;
import com.tissue.application.dto.response.RefreshTokenResponse;
import com.tissue.application.port.repository.AuthIdentityRepository;
import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.application.service.AuthenticationService;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.jwt.JwtTokenException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuthenticationServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private AuthIdentityRepository authIdentityRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Member member;
    private String password = "password1234";

    @BeforeEach
    void setUp() {
        member = Member.create("test@test.com", "testuser", "Test User");
        memberCommandRepository.save(member);

        AuthenticationIdentity authenticationIdentity =
                AuthenticationIdentity.createEmailIdentity(member, "test@test.com", passwordEncoder.encode(password));

        authIdentityRepository.save(authenticationIdentity);
    }

    @Test
    @DisplayName("Login with valid credentials returns access and refresh token")
    void loginSuccess() {
        // when
        LoginResponse response = authenticationService.login("test@test.com", password);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(refreshTokenRepository.findByEmail("test@test.com")).isPresent();
    }

    @Test
    @DisplayName("Login with invalid password throws exception")
    void loginFailInvalidPassword() {
        // when & then
        assertThatThrownBy(() -> authenticationService.login("test@test.com", "wrongpassword"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Refresh token returns new tokens")
    void refreshTokenSuccess() {
        // given
        LoginResponse loginResponse = authenticationService.login("test@test.com", password);
        String refreshToken = loginResponse.refreshToken();

        // when
        RefreshTokenResponse refreshResponse = authenticationService.refreshToken(refreshToken);

        // then
        assertThat(refreshResponse.accessToken()).isNotNull();
        assertThat(refreshResponse.refreshToken()).isNotNull();
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Using invalid refresh token throws exception")
    void refreshTokenInvalid() {
        // given
        String invalidToken = "malformed.token.value";

        // when & then
        assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("Logout deletes refresh token")
    void logoutSuccess() {
        // given
        authenticationService.login("test@test.com", password);
        assertThat(refreshTokenRepository.findByEmail("test@test.com")).isPresent();

        // when
        authenticationService.logout("test@test.com");

        // then
        assertThat(refreshTokenRepository.findByEmail("test@test.com")).isEmpty();
    }
}
