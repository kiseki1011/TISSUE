package com.tissue.tissue.security.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.authentication.application.dto.response.ElevatedTokenResponse;
import com.tissue.authentication.application.dto.response.LoginResponse;
import com.tissue.authentication.application.dto.response.RefreshTokenResponse;
import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.authentication.application.service.AuthenticationService;
import com.tissue.global.security.exception.JwtTokenException;
import com.tissue.global.security.principal.MemberDetails;
import com.tissue.global.security.principal.MemberDetailsService;
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

    @InjectMocks
    AuthenticationService sut;

    @Nested
    @DisplayName("login")
    class Login {
        @Test
        @DisplayName("success: authenticates and returns tokens")
        void success_Login() {
            String email = "test@tissue.com";
            String password = "password";
            Long memberId = 1L;
            String accessToken = "accessTokenValue";
            String refreshToken = "refreshTokenValue";

            Authentication authentication = mock(Authentication.class);
            MemberDetails memberDetails = mock(MemberDetails.class);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(memberDetails);
            given(memberDetails.getMemberId()).willReturn(memberId);
            given(memberDetails.getEmail()).willReturn(email);
            given(memberDetails.getAuthorities()).willReturn(Collections.emptyList());

            given(tokenProvider.createAccessToken(eq(memberId), eq(email), any()))
                    .willReturn(accessToken);
            given(tokenProvider.createRefreshToken(eq(memberId), eq(email), any()))
                    .willReturn(refreshToken);
            given(tokenProvider.getRefreshTokenValidityInSeconds()).willReturn(3600L);

            LoginResponse response = sut.login(email, password);

            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);

            then(refreshTokenRepository).should().save(eq(email), eq(refreshToken), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("refresh token")
    class RefreshToken {
        @Test
        @DisplayName("success: validates and rotates tokens")
        void success_RefreshToken() {
            String oldRefreshToken = "oldRefreshToken";
            String email = "test@tissue.com";
            Long memberId = 1L;
            String newAccessToken = "newAccessTokenValue";
            String newRefreshToken = "newRefreshTokenValue";

            given(tokenProvider.getSubjectFromToken(oldRefreshToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.of(oldRefreshToken));

            MemberDetails memberDetails = mock(MemberDetails.class);
            given(userDetailsService.loadUserByUsername(email)).willReturn(memberDetails);
            given(memberDetails.getMemberId()).willReturn(memberId);
            given(memberDetails.getEmail()).willReturn(email);
            given(memberDetails.getAuthorities()).willReturn(Collections.emptyList());

            given(tokenProvider.createAccessToken(eq(memberId), eq(email), any()))
                    .willReturn(newAccessToken);
            given(tokenProvider.createRefreshToken(eq(memberId), eq(email), any()))
                    .willReturn(newRefreshToken);
            given(tokenProvider.getRefreshTokenValidityInSeconds()).willReturn(3600L);

            RefreshTokenResponse response = sut.refreshToken(oldRefreshToken);

            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);

            then(tokenProvider).should().validateRefreshToken(oldRefreshToken);
            then(refreshTokenRepository).should().save(eq(email), eq(newRefreshToken), any(Duration.class));
        }

        @Test
        @DisplayName("fail: token not found in storage")
        void fail_TokenNotFound() {
            String refreshToken = "refreshToken";
            String email = "test@tissue.com";

            given(tokenProvider.getSubjectFromToken(refreshToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.refreshToken(refreshToken)).isInstanceOf(JwtTokenException.class);
        }

        @Test
        @DisplayName("fail: token reuse detected")
        void fail_TokenReuse() {
            String incomingToken = "stolenToken";
            String storedToken = "latestToken";
            String email = "test@tissue.com";

            given(tokenProvider.getSubjectFromToken(incomingToken)).willReturn(email);
            given(refreshTokenRepository.findByEmail(email)).willReturn(Optional.of(storedToken));

            assertThatThrownBy(() -> sut.refreshToken(incomingToken))
                    .isInstanceOf(JwtTokenException.class)
                    .hasMessageContaining("Refresh token reuse detected");

            then(refreshTokenRepository).should().deleteByEmail(email);
        }
    }

    @Nested
    @DisplayName("elevate permission")
    class ElevatePermission {
        @Test
        @DisplayName("success: authenticates and returns elevated token")
        void success_ElevatePermission() {
            String email = "test@tissue.com";
            String password = "password";
            Long memberId = 1L;
            String elevatedToken = "elevatedTokenValue";

            Authentication authentication = mock(Authentication.class);
            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getAuthorities()).willReturn(Collections.emptyList());
            given(tokenProvider.createElevatedToken(eq(memberId), eq(email), any()))
                    .willReturn(elevatedToken);

            ElevatedTokenResponse response = sut.elevatePermission(email, password, memberId);

            assertThat(response.elevatedToken()).isEqualTo(elevatedToken);
            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {
        @Test
        @DisplayName("success: deletes refresh token")
        void success_Logout() {
            String email = "test@tissue.com";

            sut.logout(email);

            then(refreshTokenRepository).should().deleteByEmail(email);
        }
    }
}
