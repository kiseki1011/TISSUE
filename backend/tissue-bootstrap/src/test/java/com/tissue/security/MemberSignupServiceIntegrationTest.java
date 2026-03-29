package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.service.MemberSignupService;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberSignupServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberSignupService memberSignupService;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Autowired
    private TissueSecurityProperties tissueSecurityProperties;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @AfterEach
    void tearDown() {
        tissueSecurityProperties.setEmailRequired(true);
    }

    @Nested
    @DisplayName("email signup with verified email")
    class EmailSignup {

        @Test
        @DisplayName("creates member and 'EMAIL' authentication identity with verified email")
        void successWithEmailVerification() {
            // given
            String email = "test@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("signupuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getEmail()).isEqualTo(email);
            assertThat(savedMember.getUsername()).isEqualTo("signupuser");
            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, email))
                    .isPresent();

            // verified token is consumed
            assertThat(emailVerificationRepository.validateVerifiedToken(verifiedToken))
                    .isNull();
        }

        @Test
        @DisplayName("fails when email is not verified")
        void failsWithUnverifiedEmail() {
            // given
            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email("unverified@tissue.com")
                    .verifiedToken("invalid-token")
                    .username("newuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberSignupService.signup(command)).isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("fails when email is already registered")
        void failsWithDuplicateEmail() {
            // given
            String email = "duplicate@tissue.com";
            createMemberWithEmail(email, "existinguser");

            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("newuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberSignupService.signup(command)).isInstanceOf(ResourceConflictException.class);
        }

        @Test
        @DisplayName("fails when username is already taken")
        void failsWithDuplicateUsername() {
            // given
            createMemberWithEmail("first@tissue.com", "takenuser");

            String verifiedToken = simulateEmailVerification("second@tissue.com");

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email("second@tissue.com")
                    .verifiedToken(verifiedToken)
                    .username("takenuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberSignupService.signup(command)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("username signup without email verification")
    class UsernameSignup {

        @Test
        @DisplayName("creates member with 'USERNAME' authentication identity when email is not required")
        void successWithUsernameOnly() {
            // given
            tissueSecurityProperties.setEmailRequired(false);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .username("signupuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getEmail()).isNull();
            assertThat(savedMember.getUsername()).isEqualTo("signupuser");
            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.USERNAME, "signupuser"))
                    .isPresent();
        }

        @Test
        @DisplayName("fails when username is already taken")
        void failsWithDuplicateUsername() {
            // given
            tissueSecurityProperties.setEmailRequired(false);
            memberCommandRepository.save(Member.createWithoutEmail("takenuser", "Existing"));

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .username("takenuser")
                    .password("password1234!")
                    .name("Hong Gildong")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberSignupService.signup(command)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("OAuth signup")
    class OAuthSignup {

        @Test
        @DisplayName("creates member with valid register token")
        void successWithOAuth() {
            // given
            String email = "test@tissue.com";
            String providerId = "google-456";
            String registerToken =
                    tokenProvider.createRegisterToken(AuthenticationIdentityProvider.GOOGLE.name(), providerId, email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "OAuth User");

            // when
            OAuthSignupResponse response = memberSignupService.signupWithOAuth(command);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(memberQueryRepository.findByEmail(email)).isPresent();
            assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.GOOGLE, providerId))
                    .isPresent();
        }

        @Test
        @DisplayName("fails when email is already registered by another member")
        void failsWithDuplicateEmail() {
            // given
            String email = "duplicate@tissue.com";
            createMemberWithEmail(email, "duplicateemail");

            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-456", email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauythuser", "New User");

            // when & then
            assertThatThrownBy(() -> memberSignupService.signupWithOAuth(command))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        @DisplayName("fails when username is already taken")
        void failsWithDuplicateUsername() {
            // given
            createMemberWithEmail("test1@tissue.com", "duplicateuser");

            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-456", "test2@tissue.com");

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "duplicateuser", "New User");

            // when & then
            assertThatThrownBy(() -> memberSignupService.signupWithOAuth(command))
                    .isInstanceOf(ResourceConflictException.class);
        }
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

    private void createMemberWithEmail(String email, String username) {
        Member member = Member.create(email, username, "Hong GilDong");
        memberCommandRepository.save(member);
    }
}
