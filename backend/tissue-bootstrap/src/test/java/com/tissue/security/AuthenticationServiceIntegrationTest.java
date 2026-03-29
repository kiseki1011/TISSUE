package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.service.AuthenticationService;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.jwt.JwtTokenException;
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
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.create("test@tissue.com", "testuser", "TestUser");
        memberCommandRepository.save(member);

        AuthenticationIdentity authenticationIdentity = AuthenticationIdentity.createEmailIdentity(
                member, "test@tissue.com", passwordEncoder.encode("password1234"));

        authenticationIdentityRepository.save(authenticationIdentity);
    }

    @Test
    @DisplayName("login with valid credentials returns access and refresh token")
    void loginSuccess() {
        // given
        String password = "password1234";

        // when
        LoginResponse response = authenticationService.login("test@tissue.com", password, "127.0.0.1");

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isPresent();
    }

    @Test
    @DisplayName("login fails with invalid password")
    void loginFailInvalidPassword() {
        // when & then
        assertThatThrownBy(() -> authenticationService.login("test@tissue.com", "wrongpassword", "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login fails if withdrawed member")
    void loginFailWhenWithdrawMember() {
        // given
        member.withdraw();
        String password = "password1234";

        // when & then
        assertThatThrownBy(() -> authenticationService.login("test@tissue.com", password, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("refreshing token returns new token pair")
    void refreshTokenSuccess() {
        // given
        String password = "password1234";
        LoginResponse loginResponse = authenticationService.login("test@tissue.com", password, "127.0.0.1");
        String refreshToken = loginResponse.refreshToken();

        // when
        RefreshTokenResponse refreshResponse = authenticationService.refreshToken(refreshToken);

        // then
        assertThat(refreshResponse.accessToken()).isNotNull();
        assertThat(refreshResponse.refreshToken()).isNotNull();
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("fails refreshing token with invalid refresh token")
    void refreshTokenInvalid() {
        // given
        String invalidToken = "malformed.token.value";

        // when & then
        assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("logout deletes refresh token")
    void logoutSuccess() {
        // given
        String password = "password1234";
        authenticationService.login("test@tissue.com", password, "127.0.0.1");
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isPresent();

        // when
        authenticationService.logout(member.getId());

        // then
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isEmpty();
    }
}
