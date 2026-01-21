package com.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.creator.AuthIdentityManager;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.domain.exception.InvalidTokenException;
import com.tissue.security.authentication.infrastructure.jwt.JwtTokenProvider;
import com.tissue.security.authentication.presentation.dto.response.OAuthSignupResponse;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import io.jsonwebtoken.Claims;
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
    JwtTokenProvider jwtTokenProvider;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    ProjectMemberQueryRepository projectMemberQueryRepository;

    @Mock
    WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @InjectMocks
    MemberCommandService sut;

    @Nested
    @DisplayName("signup with email")
    class Signup {
        @Test
        @DisplayName("success: creates member and identity, clears verification")
        void success_Signup() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthProvider.EMAIL)
                    .email("test@tissue.com")
                    .verificationToken("token")
                    .username("testuser")
                    .password("password")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verificationToken()))
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
            then(memberEmailVerificationService).should().clearVerification(cmd.email());
        }

        @Test
        @DisplayName("fail: verification token invalid")
        void fail_TokenInvalid() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .email("test@tissue.com")
                    .verificationToken("invalid")
                    .username("testuser")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verificationToken()))
                    .willReturn(false);

            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(InvalidTokenException.class);
            then(memberCommandRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fail: duplicate (DataIntegrityViolation)")
        void fail_DuplicationConflict() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthProvider.EMAIL)
                    .email("test@tissue.com")
                    .verificationToken("token")
                    .username("testuser")
                    .password("pass")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verificationToken()))
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

            Claims claims = mock(Claims.class);
            given(claims.get(JwtTokenProvider.CLAIM_PROVIDER, String.class)).willReturn("GOOGLE");
            given(claims.get(JwtTokenProvider.CLAIM_IDENTIFIER, String.class)).willReturn("sub123");
            given(claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class)).willReturn("google@test.com");
            given(jwtTokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(savedMember.getEmail()).willReturn("google@test.com");
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);

            AuthIdentity authIdentity = mock(AuthIdentity.class);
            given(authIdentityManager.create(savedMember, AuthProvider.GOOGLE, "sub123", null))
                    .willReturn(authIdentity);

            given(jwtTokenProvider.createAccessToken(1L, "google@test.com")).willReturn("access");
            given(jwtTokenProvider.createRefreshToken(1L, "google@test.com")).willReturn("refresh");

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

            Claims claims = mock(Claims.class);
            given(claims.get(JwtTokenProvider.CLAIM_PROVIDER, String.class)).willReturn("GITHUB");
            given(claims.get(JwtTokenProvider.CLAIM_IDENTIFIER, String.class)).willReturn("gh123");
            given(jwtTokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

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

            Claims claims = mock(Claims.class);
            given(claims.get(JwtTokenProvider.CLAIM_PROVIDER, String.class)).willReturn("GITHUB");
            given(claims.get(JwtTokenProvider.CLAIM_IDENTIFIER, String.class)).willReturn("gh123");
            given(claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class)).willReturn("gh@test.com");
            given(jwtTokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

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
        @DisplayName("success: updates email and clears verification")
        void success_UpdateEmail() {
            Long memberId = 1L;
            String newEmail = "new@tissue.com";

            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("old@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(memberEmailVerificationService.isEmailVerified(newEmail)).willReturn(true);

            sut.updateEmail(newEmail, memberId);

            then(memberValidator).should().ensureUniqueEmail(newEmail);
            then(member).should().updateEmail(newEmail);
            then(authIdentityRepository).should().findByProviderAndIdentifier(AuthProvider.EMAIL, "old@tissue.com");
            then(memberEmailVerificationService).should().clearVerification(newEmail);
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
            then(workspaceMemberQueryRepository).should().softDeleteAllByMemberId(memberId);
            then(projectMemberQueryRepository).should().softDeleteAllByMemberId(memberId);
        }
    }
}
