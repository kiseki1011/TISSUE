package com.tissue.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.authentication.application.dto.response.OAuthSignupResponse;
import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.authentication.application.port.out.TokenClaims;
import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.MemberCommandService;
import com.tissue.member.application.service.MemberEmailVerificationService;
import com.tissue.member.application.service.MemberFinder;
import com.tissue.member.application.service.MemberValidator;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.creator.AuthIdentityManager;
import com.tissue.member.domain.exception.EmailNotVerifiedException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock
    MemberFinder memberFinder;

    @Mock
    MemberCommandRepository memberCommandRepository;

    @Mock
    AuthIdentityRepository authIdentityRepository;

    @Mock
    AuthIdentityManager authIdentityManager;

    @Mock
    MemberValidator memberValidator;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @Mock
    TokenProvider tokenProvider;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    MemberCommandService sut;

    @Nested
    @DisplayName("signup with email")
    class Signup {
        @Test
        @DisplayName("success: creates member and identity, consumes verification token")
        void success_Signup() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthProvider.EMAIL)
                    .email("test@tissue.com")
                    .signupToken("validToken")
                    .username("testuser")
                    .password("password")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.validateSignupToken(cmd.email(), cmd.signupToken()))
                    .willReturn(true);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);

            AuthIdentity authIdentity = mock(AuthIdentity.class);
            given(authIdentityManager.create(savedMember, AuthProvider.EMAIL, cmd.email(), cmd.password()))
                    .willReturn(authIdentity);

            MemberSignupResponse response = sut.signup(cmd);

            assertThat(response.memberId()).isEqualTo(1L);
            then(memberValidator).should().ensureUniqueEmail(cmd.email());
            then(memberValidator).should().ensureUniqueUsername(cmd.username());
            then(authIdentityRepository).should().save(authIdentity);
        }

        @Test
        @DisplayName("fail: signup token invalid")
        void fail_TokenInvalid() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .email("test@tissue.com")
                    .signupToken("invalidToken")
                    .username("testuser")
                    .build();

            given(memberEmailVerificationService.validateSignupToken(cmd.email(), cmd.signupToken()))
                    .willReturn(false);

            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(EmailNotVerifiedException.class);
            then(memberCommandRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fail: duplicate (DataIntegrityViolation)")
        void fail_DuplicationConflict() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthProvider.EMAIL)
                    .email("test@tissue.com")
                    .signupToken("validToken")
                    .username("testuser")
                    .password("pass")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.validateSignupToken(cmd.email(), cmd.signupToken()))
                    .willReturn(true);
            given(memberCommandRepository.save(any(Member.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate"));

            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("signup with OAuth")
    class SignupOAuth {
        @Test
        @DisplayName("success: creates member and identity, returns login tokens")
        void success() {
            String registerToken = "regToken";
            SignupOAuthMemberCommand cmd = new SignupOAuthMemberCommand(registerToken, "testuser", "name");

            TokenClaims claims = TokenClaims.builder()
                    .provider("GOOGLE")
                    .identifier("sub123")
                    .email("google@test.com")
                    .build();
            given(tokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(savedMember.getEmail()).willReturn("google@test.com");
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);

            AuthIdentity authIdentity = mock(AuthIdentity.class);
            given(authIdentityManager.create(savedMember, AuthProvider.GOOGLE, "sub123", null))
                    .willReturn(authIdentity);
            given(savedMember.getRole()).willReturn(com.tissue.global.security.SystemRole.USER);

            given(tokenProvider.createAccessToken(eq(1L), eq("google@test.com"), any(), any()))
                    .willReturn("access");
            given(tokenProvider.createRefreshToken(eq(1L), eq("google@test.com"), any(), any()))
                    .willReturn("refresh");

            OAuthSignupResponse response = sut.signupOAuth(cmd);

            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");

            then(memberValidator).should().ensureUniqueUsername("testuser");
            then(memberValidator).should().ensureUniqueEmail("google@test.com");
            then(authIdentityRepository).should().save(authIdentity);
            then(refreshTokenRepository).should().save(eq("google@test.com"), eq("refresh"), any());
        }
    }

    @Nested
    @DisplayName("link OAuth account")
    class LinkOAuthAccount {
        @Test
        @DisplayName("success: links oauth account to existing member")
        void success_LinkOAuth() {
            Long memberId = 1L;
            String registerToken = "regToken";

            TokenClaims claims =
                    TokenClaims.builder().provider("GITHUB").identifier("gh123").build();
            given(tokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            given(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.GITHUB, "gh123"))
                    .willReturn(Optional.empty());

            AuthIdentity authIdentity = mock(AuthIdentity.class);
            given(authIdentityManager.create(member, AuthProvider.GITHUB, "gh123", null))
                    .willReturn(authIdentity);

            sut.linkOAuthAccount(registerToken, memberId);

            then(authIdentityRepository).should().save(authIdentity);
        }

        @Test
        @DisplayName("fail: identity already linked")
        void fail_AlreadyLinked() {
            Long memberId = 1L;
            String registerToken = "regToken";

            TokenClaims claims = TokenClaims.builder()
                    .provider("GITHUB")
                    .identifier("gh123")
                    .email("gh@test.com")
                    .build();
            given(tokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            given(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.GITHUB, "gh123"))
                    .willReturn(Optional.of(mock(AuthIdentity.class)));

            assertThatThrownBy(() -> sut.linkOAuthAccount(registerToken, memberId))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("link email account")
    class LinkEmail {
        @Test
        @DisplayName("success: adds password identity")
        void success_LinkEmail() {
            Long memberId = 1L;
            String newPassword = "newPassword";
            Member member = mock(Member.class);

            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            given(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.empty());

            given(passwordEncoder.encode(newPassword)).willReturn("encoded");

            sut.addPassword(newPassword, memberId);

            then(authIdentityRepository).should().save(any(AuthIdentity.class));
        }

        @Test
        @DisplayName("fail: password identity already exists")
        void fail_AlreadyExists() {
            Long memberId = 1L;
            Member member = mock(Member.class);

            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            given(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.of(mock(AuthIdentity.class)));

            assertThatThrownBy(() -> sut.addPassword("pass", memberId)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("update name")
    class UpdateName {
        @Test
        @DisplayName("success: updates name")
        void success_UpdateName() {
            Long memberId = 1L;
            String newName = "newName";

            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            sut.updateName(newName, memberId);

            then(member).should().updateName(newName);
        }
    }

    @Nested
    @DisplayName("update email")
    class UpdateEmail {
        @Test
        @DisplayName("success: updates email and consumes verification token")
        void success_UpdateEmail() {
            Long memberId = 1L;
            String newEmail = "new@tissue.com";
            String token = "validToken";

            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("old@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(memberEmailVerificationService.validateSignupToken(newEmail, token))
                    .willReturn(true);

            sut.updateEmail(newEmail, token, memberId);

            then(memberValidator).should().ensureUniqueEmail(newEmail);
            then(member).should().updateEmail(newEmail);
            then(authIdentityRepository).should().findByProviderAndIdentifier(AuthProvider.EMAIL, "old@tissue.com");
        }
    }

    @Nested
    @DisplayName("update username")
    class UpdateUsername {
        @Test
        @DisplayName("success: updates username")
        void success_UpdateUsername() {
            Long memberId = 1L;
            String newUsername = "newUserName";

            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            sut.updateUsername(newUsername, memberId);

            then(memberValidator).should().ensureUniqueUsername(newUsername);
            then(member).should().updateUsername(newUsername);
        }
    }

    @Nested
    @DisplayName("update password")
    class UpdatePassword {
        @Test
        @DisplayName("success: authenticates and updates password")
        void success_UpdatePassword() {
            Long memberId = 1L;
            String oldPass = "oldPassword";
            String newPass = "newPassword";

            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(passwordEncoder.encode(newPass)).willReturn("encodedNewPassword");

            AuthIdentity authIdentity = mock(AuthIdentity.class);
            given(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.of(authIdentity));

            sut.updatePassword(oldPass, newPass, memberId);

            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(authIdentity).should().updateCredential("encodedNewPassword");
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {
        @Test
        @DisplayName("success: authenticates, checks withdrawable, and withdraws")
        void success_Withdraw() {
            Long memberId = 1L;
            String password = "password";
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            sut.withdraw(password, memberId);

            then(authenticationManager).should().authenticate(any());
            then(memberValidator).should().ensureWithdrawable(member);
            then(member).should().withdraw();
            //            then(workspaceMemberQueryRepository).should().softDeleteAllByMemberId(memberId);
            //            then(projectMemberQueryRepository).should().softDeleteAllByMemberId(memberId);
        }
    }
}
