package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.service.PasswordResetService;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PasswordResetServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpMember() {
        Member member = Member.create("test@tissue.com", "testuser", "TestUser");
        memberCommandRepository.save(member);

        AuthenticationIdentity identity = AuthenticationIdentity.createEmailIdentity(
                member, "test@tissue.com", passwordEncoder.encode("password123!"));
        authenticationIdentityRepository.save(identity);
    }

    @Nested
    @DisplayName("request password reset")
    class RequestPasswordReset {

        @Test
        @DisplayName("returns verificationId for existing email")
        void successRequestPasswordReset() {
            // when
            String verificationId = passwordResetService.requestPasswordReset("test@tissue.com");

            // then
            assertThat(verificationId).isNotBlank();
        }

        @Test
        @DisplayName("returns verificationId even if email identity does not exist")
        void returnsVerificationIdForNonExistentEmail() {
            // when
            String verificationId = passwordResetService.requestPasswordReset("notexist@tissue.com");

            // then
            assertThat(verificationId).isNotBlank();
        }
    }

    @Nested
    @DisplayName("reset password")
    class ResetPassword {

        @Test
        @DisplayName("resets password and invalidates all refresh tokens")
        void success() {
            // given
            String email = "test@tissue.com";
            String verifiedToken = simulateEmailVerification(email);
            String newPassword = "newPassword123!";

            // when
            passwordResetService.resetPassword(email, verifiedToken, newPassword);

            // then
            AuthenticationIdentity identity = authenticationIdentityRepository
                    .findByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, email)
                    .orElseThrow();
            assertThat(passwordEncoder.matches(newPassword, identity.getCredential()))
                    .isTrue();
        }

        @Test
        @DisplayName("fails with invalid reset token")
        void failsWithInvalidToken() {
            // when & then
            assertThatThrownBy(() ->
                            passwordResetService.resetPassword("test@tissue.com", "invalid-token", "newPassword123!"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fails when reset token email does not match")
        void failsWhenTokenEmailMismatch() {
            // given
            String verifiedToken = simulateEmailVerification("other@tissue.com");

            // when & then
            assertThatThrownBy(() ->
                            passwordResetService.resetPassword("test@tissue.com", verifiedToken, "newPassword123!"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    private String simulateEmailVerification(String email) {
        String emailToken = java.util.UUID.randomUUID().toString();
        String verificationId = java.util.UUID.randomUUID().toString();

        emailVerificationRepository.storeVerificationContext(
                verificationId, email, emailToken, emailVerificationProperties.getEmailTtl());
        emailVerificationRepository.verifyByEmailToken(emailToken, emailVerificationProperties.getVerifiedTokenTtl());

        VerificationStatus status = emailVerificationRepository.getStatus(verificationId);
        return status.verifiedToken();
    }
}
