package com.tissue.security.application.service;

import static com.tissue.security.domain.exception.AuthenticationErrorCode.INVALID_PASSWORD_RESET_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private MemberFinder memberFinder;

    @Mock
    private AuthenticationIdentityRepository identityRepository;

    @Mock
    private MemberEmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private PasswordResetService sut;

    @Nested
    @DisplayName("reset password")
    class ResetPassword {

        @Test
        @DisplayName("success: resets password and deletes refresh token")
        void successResetPassword() {
            // given
            String email = "test@tissue.com";
            String verifiedToken = UUID.randomUUID().toString();
            String newPassword = "newPassword123!";
            String encodedPassword = "encoded-newPassword123!";

            Member member = Member.create(email, "testuser", "Test User");
            AuthenticationIdentity emailIdentity =
                    AuthenticationIdentity.createEmailIdentity(member, email, "oldEncodedPassword");
            AuthenticationIdentity usernameIdentity =
                    AuthenticationIdentity.createUsernameIdentity(member, "testuser", "oldEncodedPassword");

            given(emailVerificationService.isTokenVerified(email, verifiedToken))
                    .willReturn(true);
            given(memberFinder.getActiveByEmail(email)).willReturn(Optional.of(member));
            given(identityRepository.findAllByMemberIdAndProviderIn(
                            member.getId(),
                            List.of(AuthenticationIdentityProvider.EMAIL, AuthenticationIdentityProvider.USERNAME)))
                    .willReturn(List.of(emailIdentity, usernameIdentity));
            given(passwordEncoder.encode(newPassword)).willReturn(encodedPassword);

            // when
            sut.resetPassword(email, verifiedToken, newPassword);

            // then
            assertThat(emailIdentity.getCredential()).isEqualTo(encodedPassword);
            assertThat(usernameIdentity.getCredential()).isEqualTo(encodedPassword);
            then(refreshTokenRepository).should().deleteByMemberId(member.getId());
        }

        @Test
        @DisplayName("fail: if reset token is invalid, throws BadRequestException")
        void failResetPassword_If_ResetTokenInvalid() {
            // given
            String email = "test@tissue.com";
            String invalidToken = "invalid-token";
            String newPassword = "newPassword123!";

            given(emailVerificationService.isTokenVerified(email, invalidToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> sut.resetPassword(email, invalidToken, newPassword))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(INVALID_PASSWORD_RESET_TOKEN);

            then(memberFinder).should(never()).getActiveByEmail(any());
            then(refreshTokenRepository).should(never()).deleteByMemberId(any());
        }
    }

    @Nested
    @DisplayName("request password reset")
    class RequestPasswordReset {

        @Test
        @DisplayName("success: sends verification email for existing email")
        void successRequestPasswordReset() {
            // given
            String email = "test@tissue.com";
            String expectedVerificationId = UUID.randomUUID().toString();

            given(identityRepository.existsByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, email))
                    .willReturn(true);
            given(emailVerificationService.sendPasswordResetVerificationEmail(email))
                    .willReturn(expectedVerificationId);

            // when
            String result = sut.requestPasswordReset(email);

            // then
            assertThat(result).isEqualTo(expectedVerificationId);
            then(rateLimitService).should().checkPasswordResetRateLimit(email);
        }

        @Test
        @DisplayName("success: returns verificationId without sending email for non existent email")
        void successRequestPasswordReset_If_EmailNotExists() {
            // given
            String email = "nonexistent@tissue.com";

            given(identityRepository.existsByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, email))
                    .willReturn(false);

            // when
            String result = sut.requestPasswordReset(email);

            // then
            assertThat(result).isNotBlank();
            then(emailVerificationService).should(never()).sendPasswordResetVerificationEmail(any());
        }
    }
}
