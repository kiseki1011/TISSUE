package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.service.AuthenticationService;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.exception.TokenReuseDetectedException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class RefreshTokenReuseIntegrationTest extends IntegrationTestSupport {

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

    @Test
    @DisplayName("reusing a rotated refresh token revokes the stored token and it stays revoked")
    void reuseRevokesStoredTokenAndPersists() {
        // given - login, then rotate once so the first refresh token is now stale
        Member member = createMember();
        String firstRefreshToken = authenticationService
                .login("test@tissue.com", "password1234", "127.0.0.1")
                .refreshToken();
        RefreshTokenResponse rotated = authenticationService.refreshToken(firstRefreshToken);
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isPresent();

        // when
        assertThatThrownBy(() -> authenticationService.refreshToken(firstRefreshToken))
                .isInstanceOf(TokenReuseDetectedException.class);

        // then
        assertThat(refreshTokenRepository.findByMemberId(member.getId())).isEmpty();
        assertThatThrownBy(() -> authenticationService.refreshToken(rotated.refreshToken()))
                .isInstanceOf(RuntimeException.class);
    }

    private Member createMember() {
        Member member = memberCommandRepository.save(Member.create("test@tissue.com", "testuser", "TestUser"));
        authenticationIdentityRepository.save(AuthenticationIdentity.createEmailIdentity(
                member, "test@tissue.com", passwordEncoder.encode("password1234")));
        return member;
    }
}
