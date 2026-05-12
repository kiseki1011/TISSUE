package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.service.MemberSignupService;
import com.tissue.security.config.DeploymentProperties;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.security.domain.exception.SignupBlockedNoWorkspaceException;
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

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Autowired
    private DeploymentProperties deploymentProperties;

    @AfterEach
    void tearDown() {
        tissueSecurityProperties.setEmailRequired(true);
        deploymentProperties.setMultiTenant(false);
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
            createSetupWorkspace();

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
            createSetupWorkspace();

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
            createSetupWorkspace();

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
            createSetupWorkspace();

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
            createSetupWorkspace();

            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-456", "test2@tissue.com");

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "duplicateuser", "New User");

            // when & then
            assertThatThrownBy(() -> memberSignupService.signupWithOAuth(command))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("first user role assignment")
    class FirstUserRoleAssignment {

        @Test
        @DisplayName("first signed-up member becomes SystemRole.ADMIN")
        void firstSignupBecomesAdmin() {
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
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.ADMIN);
        }

        @Test
        @DisplayName("subsequent signup gets SystemRole.USER")
        void subsequentSignupBecomesUser() {
            // given
            memberCommandRepository.save(Member.create("existing@tissue.com", "existing", "Existing"));
            createSetupWorkspace();

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
        @DisplayName("username-only signup: first user becomes ADMIN")
        void firstUsernameOnlySignupBecomesAdmin() {
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
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.ADMIN);
        }

        @Test
        @DisplayName("OAuth signup: first user becomes ADMIN")
        void firstOAuthSignupBecomesAdmin() {
            // given
            String email = "oauthfirst@tissue.com";
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-first", email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthadmin", "OAuth Admin");

            // when
            OAuthSignupResponse response = memberSignupService.signupWithOAuth(command);

            // then
            Member savedMember = memberQueryRepository.findByEmail(email).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.ADMIN);
            assertThat(response.accessToken()).isNotNull();
        }

        @Test
        @DisplayName("OAuth signup: subsequent user becomes USER")
        void subsequentOAuthSignupBecomesUser() {
            // given
            memberCommandRepository.save(Member.create("seed@tissue.com", "seed", "Seed"));
            createSetupWorkspace();

            String email = "oauthnew@tissue.com";
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-new", email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthnew", "OAuth New");

            // when
            memberSignupService.signupWithOAuth(command);

            // then
            Member savedMember = memberQueryRepository.findByEmail(email).orElseThrow();
            assertThat(savedMember.getRole()).isEqualTo(SystemRole.USER);
        }
    }

    @Nested
    @DisplayName("single-tenant mode signup gating")
    class SingleTenantSignupGating {

        @Test
        @DisplayName("first user can sign up when workspace=0 and members=0")
        void firstUserCanSignupWithNoWorkspace() {
            // given (clean DB - default by IntegrationTestSupport.setUp())

            String email = "first@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("firstuser")
                    .password("password1234!")
                    .name("First")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            assertThat(memberQueryRepository.findById(response.memberId())).isPresent();
        }

        @Test
        @DisplayName("subsequent signup is blocked when workspace=0 and members>0")
        void subsequentSignupBlockedWithoutWorkspace() {
            // given
            memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));

            String email = "next@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("nextuser")
                    .password("password1234!")
                    .name("Next")
                    .build();

            // when / then
            assertThatThrownBy(() -> memberSignupService.signup(command))
                    .isInstanceOf(SignupBlockedNoWorkspaceException.class);
        }

        @Test
        @DisplayName("OAuth signup is also blocked when workspace=0 and members>0")
        void oauthSignupBlockedWithoutWorkspace() {
            // given
            memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));

            String email = "oauth@tissue.com";
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-blocked", email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "OAuth User");

            // when / then
            assertThatThrownBy(() -> memberSignupService.signupWithOAuth(command))
                    .isInstanceOf(SignupBlockedNoWorkspaceException.class);
        }
    }

    @Nested
    @DisplayName("single-tenant mode auto-join")
    class SingleTenantAutoJoin {

        @Test
        @DisplayName("signup auto-joins as MEMBER when exactly 1 workspace exists")
        void autoJoinsWhenSingleWorkspace() {
            // given
            Member admin = memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));
            Workspace workspace = workspaceRepository.save(Workspace.create("only-ws", "Only", "desc"));

            String email = "joiner@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("joiner")
                    .password("password1234!")
                    .name("Joiner")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member joined = memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(workspace, joined))
                    .isPresent()
                    .get()
                    .satisfies(wm -> assertThat(wm.getRole()).isEqualTo(WorkspaceRole.MEMBER));

            // admin did not auto-join (was created directly)
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(workspace, admin))
                    .isEmpty();
        }

        @Test
        @DisplayName("signup does NOT auto-join when 2 or more workspaces exist")
        void noAutoJoinWhenMultipleWorkspaces() {
            // given
            memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));
            Workspace ws1 = workspaceRepository.save(Workspace.create("ws-one", "WS1", "desc"));
            Workspace ws2 = workspaceRepository.save(Workspace.create("ws-two", "WS2", "desc"));

            String email = "joiner@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("joiner")
                    .password("password1234!")
                    .name("Joiner")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member joined = memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(ws1, joined))
                    .isEmpty();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(ws2, joined))
                    .isEmpty();
        }

        @Test
        @DisplayName("OAuth signup also auto-joins when 1 workspace exists")
        void oauthSignupAutoJoins() {
            // given
            memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));
            Workspace workspace = workspaceRepository.save(Workspace.create("oauth-ws", "OAuth WS", "desc"));

            String email = "oauth@tissue.com";
            String registerToken = tokenProvider.createRegisterToken(
                    AuthenticationIdentityProvider.GOOGLE.name(), "google-join", email);

            SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "OAuth User");

            // when
            memberSignupService.signupWithOAuth(command);

            // then
            Member joined = memberQueryRepository.findByEmail(email).orElseThrow();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(workspace, joined))
                    .isPresent()
                    .get()
                    .satisfies(wm -> assertThat(wm.getRole()).isEqualTo(WorkspaceRole.MEMBER));
        }
    }

    @Nested
    @DisplayName("multi-tenant mode")
    class MultiTenantMode {

        @Test
        @DisplayName("signup is allowed when workspace=0 and members>0")
        void signupAllowedEvenWithoutWorkspace() {
            // given
            deploymentProperties.setMultiTenant(true);
            memberCommandRepository.save(Member.create("seed@tissue.com", "seeduser", "Seed"));

            String email = "second@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("seconduser")
                    .password("password1234!")
                    .name("Second")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            assertThat(memberQueryRepository.findById(response.memberId())).isPresent();
        }

        @Test
        @DisplayName("signup does NOT auto-join even when 1 workspace exists")
        void noAutoJoinInMultiTenant() {
            // given
            deploymentProperties.setMultiTenant(true);
            memberCommandRepository.save(Member.create("admin@tissue.com", "adminuser", "Admin"));
            Workspace workspace = workspaceRepository.save(Workspace.create("multi-ws", "Multi WS", "desc"));

            String email = "joiner@tissue.com";
            String verifiedToken = simulateEmailVerification(email);

            SignupMemberCommand command = SignupMemberCommand.builder()
                    .email(email)
                    .verifiedToken(verifiedToken)
                    .username("joiner")
                    .password("password1234!")
                    .name("Joiner")
                    .build();

            // when
            MemberSignupResponse response = memberSignupService.signup(command);

            // then
            Member joined = memberQueryRepository.findById(response.memberId()).orElseThrow();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(workspace, joined))
                    .isEmpty();
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

    private Workspace createSetupWorkspace() {
        return workspaceRepository.save(Workspace.create("setup-ws", "Setup", null));
    }
}
