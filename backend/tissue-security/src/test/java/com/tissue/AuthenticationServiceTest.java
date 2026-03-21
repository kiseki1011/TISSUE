package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.security.application.dto.response.ElevatedTokenResponse;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.service.AuthenticationService;
import com.tissue.security.application.service.RateLimitService;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.RefreshTokenNotFoundException;
import com.tissue.security.domain.exception.TokenReuseDetectedException;
import com.tissue.security.principal.MemberDetails;
import com.tissue.security.principal.MemberDetailsService;
import java.time.Duration;
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
    MemberDetailsService userDetailsService;

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

            given(tokenProvider.createAccessToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(accessToken);
            given(tokenProvider.createRefreshToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(refreshToken);
            given(tokenProvider.getRefreshTokenValidity()).willReturn(Duration.ofHours(1));

            // when
            LoginResponse response = sut.login(email, password, "127.0.0.1");

            // then
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);

            then(refreshTokenRepository).should().save(eq(email), eq(refreshToken), any(Duration.class));
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

            given(tokenProvider.getSubjectFromToken(oldRefreshToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.of(oldRefreshToken));

            MemberDetails memberDetails = new MemberDetails(memberId, email, username, Collections.emptyList());
            given(userDetailsService.loadUserByUsername(email)).willReturn(memberDetails);

            given(tokenProvider.createAccessToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(newAccessToken);
            given(tokenProvider.createRefreshToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(newRefreshToken);
            given(tokenProvider.getRefreshTokenValidity()).willReturn(Duration.ofHours(1));

            // when
            RefreshTokenResponse response = sut.refreshToken(oldRefreshToken);

            // then
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);

            then(tokenProvider).should().validateRefreshToken(oldRefreshToken);
            then(refreshTokenRepository).should().save(eq(email), eq(newRefreshToken), any(Duration.class));
        }

        @Test
        @DisplayName("fail: refresh token not found in storage")
        void failRefreshTokenNotFound() {
            // given
            String refreshToken = "refreshToken";
            String email = "test@tissue.com";

            given(tokenProvider.getSubjectFromToken(refreshToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willThrow(RefreshTokenNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> sut.refreshToken(refreshToken)).isInstanceOf(RefreshTokenNotFoundException.class);
        }

        @Test
        @DisplayName("fail: refresh token reuse detected")
        void failRefreshTokenReuse() {
            // given
            String incomingToken = "stolenToken";
            String storedToken = "latestToken";
            String email = "test@tissue.com";

            given(tokenProvider.getSubjectFromToken(incomingToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> sut.refreshToken(incomingToken)).isInstanceOf(TokenReuseDetectedException.class);

            then(refreshTokenRepository).should().deleteByEmail(email);
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
            String elevatedToken = "elevatedTokenValue";

            MemberDetails memberDetails = new MemberDetails(memberId, email, username, Collections.emptyList());

            Authentication authentication = mock(Authentication.class);
            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(userDetailsService.loadUserByUsername(email)).willReturn(memberDetails);
            given(authentication.getAuthorities()).willReturn(Collections.emptyList());
            given(tokenProvider.createElevatedToken(eq(memberId), eq(email), eq(username), any()))
                    .willReturn(elevatedToken);

            // when
            ElevatedTokenResponse response = sut.elevatePermission(email, password);

            // then
            assertThat(response.elevatedToken()).isEqualTo(elevatedToken);
            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {
        @Test
        @DisplayName("success: deletes refresh token")
        void successLogout() {
            // given
            String email = "test@tissue.com";

            // when
            sut.logout(email);

            // then
            then(refreshTokenRepository).should().deleteByEmail(email);
        }
    }
}
