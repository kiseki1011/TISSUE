package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.response.ElevatedTokenResponse;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.service.AuthenticationService;
import com.tissue.security.application.service.RateLimitService;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.RefreshTokenNotFoundException;
import com.tissue.security.domain.exception.TokenReuseDetectedException;
import com.tissue.security.principal.MemberDetails;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    TokenProvider tokenProvider;

    @Mock
    TokenPairCreateService tokenPairCreateService;

    @Mock
    MemberFinder memberFinder;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    RateLimitService rateLimitService;

    @InjectMocks
    AuthenticationService sut;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("success: authenticates and returns tokens")
        void success_Login() {
            // given
            Long memberId = 1L;
            String email = "test@tissue.com";

            String username = "testuser";
            String password = "password";
            String accessToken = "accessTokenValue";
            String refreshToken = "refreshTokenValue";

            Authentication authentication = mock(Authentication.class);
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);

            MemberDetails memberDetails = new MemberDetails(memberId, email, username, Collections.emptyList());
            given(authentication.getPrincipal()).willReturn(memberDetails);

            given(tokenPairCreateService.createTokens(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(new TokenPair(accessToken, refreshToken));

            // when
            LoginResponse response = sut.login(email, password, "127.0.0.1");

            // then
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);

            then(tokenPairCreateService).should().createTokens(eq(memberId), eq(email), eq(username), any());
            then(rateLimitService).should().checkLoginRateLimit("127.0.0.1", email);
            then(rateLimitService).should().resetLoginAttempts("127.0.0.1", email);
        }
    }

    @Nested
    @DisplayName("refresh token")
    class RefreshToken {

        @Test
        @DisplayName("success: validates refresh token and rotates tokens")
        void successRefreshToken() {
            // given
            Long memberId = 1L;
            String email = "test@tissue.com";
            String username = "testuser";
            String oldRefreshToken = "oldRefreshToken";
            String newAccessToken = "newAccessTokenValue";
            String newRefreshToken = "newRefreshTokenValue";

            given(tokenProvider.validateRefreshTokenAndGetMemberId(oldRefreshToken))
                    .willReturn(memberId);
            given(refreshTokenRepository.findByMemberId(memberId)).willReturn(Optional.of(oldRefreshToken));

            Member member = Member.create(email, username, "Test User");
            given(memberFinder.getActiveById(memberId)).willReturn(member);

            given(tokenPairCreateService.createTokens(eq(member.getId()), eq(email), eq(username), any()))
                    .willReturn(new TokenPair(newAccessToken, newRefreshToken));

            // when
            RefreshTokenResponse response = sut.refreshToken(oldRefreshToken);

            // then
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        @DisplayName("fail: refresh token not found in storage")
        void failRefreshTokenNotFound() {
            // given
            Long memberId = 1L;
            String refreshToken = "refreshToken";

            given(tokenProvider.validateRefreshTokenAndGetMemberId(refreshToken))
                    .willReturn(memberId);
            given(refreshTokenRepository.findByMemberId(memberId)).willThrow(RefreshTokenNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> sut.refreshToken(refreshToken)).isInstanceOf(RefreshTokenNotFoundException.class);
        }

        @Test
        @DisplayName("fail: refresh token reuse detected")
        void failRefreshTokenReuse() {
            // given
            Long memberId = 1L;
            String incomingToken = "stolenToken";
            String storedToken = "latestToken";

            given(tokenProvider.validateRefreshTokenAndGetMemberId(incomingToken))
                    .willReturn(memberId);
            given(refreshTokenRepository.findByMemberId(memberId)).willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> sut.refreshToken(incomingToken)).isInstanceOf(TokenReuseDetectedException.class);

            then(refreshTokenRepository).should().deleteByMemberId(memberId);
        }
    }

    @Nested
    @DisplayName("elevate permission")
    class ElevatePermission {
        @Test
        @DisplayName("success: authenticates and returns elevated token")
        void successElevatePermission() {
            // given
            Long memberId = 1L;
            String email = "test@tissue.com";

            String username = "testuser";
            String password = "password";
            String clientIp = "127.0.0.1";
            String elevatedToken = "elevatedTokenValue";

            MemberDetails memberDetails = new MemberDetails(memberId, email, username, Collections.emptyList());

            Authentication authentication = mock(Authentication.class);
            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(memberDetails);
            given(tokenProvider.createElevatedToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(elevatedToken);

            // when
            ElevatedTokenResponse response = sut.elevatePermission(email, password, clientIp);

            // then
            assertThat(response.elevatedToken()).isEqualTo(elevatedToken);
            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(rateLimitService).should().checkLoginRateLimit(clientIp, email);
            then(rateLimitService).should().resetLoginAttempts(clientIp, email);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {
        @Test
        @DisplayName("success: deletes refresh token")
        void successLogout() {
            // given
            Long memberId = 1L;

            // when
            sut.logout(memberId);

            // then
            then(refreshTokenRepository).should().deleteByMemberId(memberId);
        }
    }
}
