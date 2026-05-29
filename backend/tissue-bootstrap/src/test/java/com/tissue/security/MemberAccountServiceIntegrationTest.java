package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.service.MemberAccountService;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberAccountServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberAccountService memberAccountService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("link email authentication identity")
    class LinkEmailAuthentication {

        @Test
        @DisplayName("creates 'EMAIL' identity for member that signed up through OAuth")
        void successEmailLinking() {
            // given
            Member member = createMemberWithOAuth("test@tissue.com", "oauthuser", "google-123");

            // when
            memberAccountService.linkEmailAuthentication("password123!", member.getId());

            // then
            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .isPresent();
        }

        @Test
        @DisplayName("fails when 'EMAIL' identity already exists")
        void failsWhenAlreadyLinked() {
            // given
            Member member = createMemberWithEmailIdentity("test@tissue.com", "testuser", "password123!");

            // when & then
            assertThatThrownBy(() -> memberAccountService.linkEmailAuthentication("password123!", member.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("link OAuth authentication identity")
    class LinkOAuthAccount {

        @Test
        @DisplayName("links OAuth identity to existing member")
        void success() {
            // given
            Member member = createMemberWithEmailIdentity("test@tissue.com", "linkuser", "password123!");
            String providerId = "github-456";
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GITHUB.name(), providerId, "test@tissue.com");

            // when
            memberAccountService.linkOAuthAccount(registerToken, member.getId());

            // then
            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.GITHUB, providerId))
                    .isPresent();
        }

        @Test
        @DisplayName("fails when OAuth identity is already linked")
        void failsWhenAlreadyLinked() {
            // given
            Member member = createMemberWithOAuth("dup@tissue.com", "dupuser", "github-456");
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GITHUB.name(), "github-456", "dup@tissue.com");

            // when & then
            assertThatThrownBy(() -> memberAccountService.linkOAuthAccount(registerToken, member.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("update email")
    class UpdateEmail {

        @Test
        @DisplayName("updates member email and 'EMAIL' authentication identity identifier")
        void success() {
            // given
            String oldMail = "old@tissue.com";
            Member member = createMemberWithEmailIdentity(oldMail, "testuser", "password123!");
            String newEmail = "new@tissue.com";
            String verifiedToken = simulateEmailVerification(newEmail);

            // when
            memberAccountService.updateEmail(newEmail, verifiedToken, member.getId());

            // then
            Member updated = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo(newEmail);

            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, newEmail))
                    .isPresent();

            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, oldMail))
                    .isEmpty();
        }

        @Test
        @DisplayName("fails when new email is not verified")
        void failsWithUnverifiedEmail() {
            // given
            Member member = createMemberWithEmailIdentity("old@tissue.com", "testuser", "password123!");

            // when & then
            assertThatThrownBy(
                            () -> memberAccountService.updateEmail("new@tissue.com", "invalid-token", member.getId()))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("fails when new email is already registered")
        void failsWithDuplicateEmail() {
            // given
            String duplicateMail = "dup@tissue.com";
            createMemberWithEmailIdentity(duplicateMail, "otheruser", "password123!");
            Member member = createMemberWithEmailIdentity("old@tissue.com", "testuser", "password123!");
            String verifiedToken = simulateEmailVerification(duplicateMail);

            // when & then
            assertThatThrownBy(() -> memberAccountService.updateEmail(duplicateMail, verifiedToken, member.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("update password")
    class UpdatePassword {

        @Test
        @DisplayName("fails when original password is wrong")
        void failsWithWrongPassword() {
            // given
            Member member = createMemberWithEmailIdentity("test@tissue.com", "testuser", "password123!");

            // when & then
            assertThatThrownBy(() ->
                            memberAccountService.updatePassword("wrongPassword!", "newPassword123!", member.getId()))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("soft deletes member when password is correct")
        void success() {
            // given
            Member member = createMemberWithEmailIdentity("test@tissue.com", "testuser", "password123!");

            // when
            memberAccountService.withdraw("password123!", member.getId());

            // then
            Member withdrawn = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.DELETED);
        }

        @Test
        @DisplayName("fails when member is the last super admin")
        void failsWhenLastSuperAdmin() {
            // given
            Member member = memberCommandRepository.save(
                    Member.createAsSuperAdmin("super@tissue.com", "superuser", "Super Admin"));
            authenticationIdentityRepository.save(AuthenticationIdentity.createEmailIdentity(
                    member, "super@tissue.com", passwordEncoder.encode("password123!")));
            em.flush();

            // when & then
            assertThatThrownBy(() -> memberAccountService.withdraw("password123!", member.getId()))
                    .isInstanceOf(LastSuperAdminException.class);
        }

        @Test
        @DisplayName("fails when password is wrong")
        void failsWithWrongPassword() {
            // given
            Member member = createMemberWithEmailIdentity("test@tissue.com", "testuser", "password123!");

            // when & then
            assertThatThrownBy(() -> memberAccountService.withdraw("wrongPassword!", member.getId()))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    private Member createMemberWithEmailIdentity(String email, String username, String rawPassword) {
        Member member = memberCommandRepository.save(Member.create(email, username, "Test User"));
        authenticationIdentityRepository.save(
                AuthenticationIdentity.createEmailIdentity(member, email, passwordEncoder.encode(rawPassword)));
        em.flush();
        return member;
    }

    private Member createMemberWithOAuth(String email, String username, String providerId) {
        Member member = memberCommandRepository.save(Member.create(email, username, "Test User"));
        authenticationIdentityRepository.save(
                AuthenticationIdentity.createSocialIdentity(member, AuthenticationIdentityProvider.GITHUB, providerId));
        em.flush();
        return member;
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
