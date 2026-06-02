package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.service.MemberSignupService;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.shared.exception.base.ForbiddenException;
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
    private SignupProperties signupProperties;

    @AfterEach
    void tearDown() {
        tissueSecurityProperties.setEmailRequired(true);
        signupProperties.setEnabled(true);
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
    @DisplayName("first user role assignment")
    class FirstUserRoleAssignment {

        @Test
        @DisplayName("first signed-up member becomes SystemRole.SUPER_ADMIN")
        void firstSignupBecomesSuperAdmin() {
            // given
            String email = "first@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("firstuser")
                    .password("password1234!")
                    .name("First Admin")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("subsequent signup gets SystemRole.USER")
        void subsequentSignupBecomesUser() {
            // given
            memberCommandRepository.save(Member.create("existing@tissue.com", "existing", "Existing"));

            String email = "second@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("seconduser")
                    .password("password1234!")
                    .name("Second User")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.USER);
        }

        @Test
        @DisplayName("username-only signup: first user becomes SUPER_ADMIN")
        void firstUsernameOnlySignupBecomesSuperAdmin() {
            // given
            tissueSecurityProperties.setEmailRequired(false);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .username("firstadmin")
                    .password("password1234!")
                    .name("First Admin")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }
    }

    @Nested
    @DisplayName("signup is closed by default")
    class SignupGating {

        @Test
        @DisplayName("blocks a non first signup when self-signup is disabled")
        void disabledBlocksNonFirstSignup() {
            // given - one member already exists and signup is closed
            tissueSecurityProperties.setEmailRequired(false);
            signupProperties.setEnabled(false);
            memberCommandRepository.save(Member.createWithoutEmail("existing", "Existing"));

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .username("blocked")
                    .password("password1234!")
                    .name("Blocked")
                    .build();

            // when & then
            assertThatThrownBy(() -> memberSignupService.signup(command)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("allows the first member to bootstrap even when self-signup is disabled")
        void firstUserAllowedWhenDisabled() {
            // given - no member signed up yet, and signup is closed
            tissueSecurityProperties.setEmailRequired(false);
            signupProperties.setEnabled(false);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .username("firstadmin")
                    .password("password1234!")
                    .name("First Admin")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member savedMember =
                    memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
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
