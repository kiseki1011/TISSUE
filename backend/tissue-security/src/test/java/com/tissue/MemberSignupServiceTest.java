package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.repository.AuthIdentityRepository;
import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.application.service.MemberAccountValidator;
import com.tissue.application.service.MemberEmailVerificationService;
import com.tissue.application.service.MemberSignupService;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.TokenClaims;
import com.tissue.domain.TokenProvider;
import com.tissue.domain.exception.EmailNotVerifiedException;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class MemberSignupServiceTest {

    @Mock
    MemberFinder memberFinder;

    @Mock
    MemberCommandRepository memberCommandRepository;

    @Mock
    AuthIdentityRepository authIdentityRepository;

    @Mock
    MemberAccountValidator memberAccountValidator;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @Mock
    TokenProvider tokenProvider;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    MemberSignupService sut;

    @Nested
    @DisplayName("signup with email")
    class Signup {
        @Test
        @DisplayName("success: creates member and identity, consumes verification token")
        void success_Signup() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthenticationProvider.EMAIL)
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
            given(passwordEncoder.encode(cmd.password())).willReturn("encodedPassword");

            MemberSignupResponse response = sut.signupWithEmail(cmd);

            assertThat(response.memberId()).isEqualTo(1L);
            then(memberAccountValidator).should().ensureUniqueEmail(cmd.email());
            then(memberAccountValidator).should().ensureUniqueUsername(cmd.username());

            then(authIdentityRepository).should().save(any());
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

            assertThatThrownBy(() -> sut.signupWithEmail(cmd)).isInstanceOf(EmailNotVerifiedException.class);
            then(memberCommandRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fail: duplicate (DataIntegrityViolation)")
        void fail_DuplicationConflict() {
            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .provider(AuthenticationProvider.EMAIL)
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

            assertThatThrownBy(() -> sut.signupWithEmail(cmd)).isInstanceOf(ResourceConflictException.class);
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
            given(savedMember.getRole()).willReturn(SystemRole.USER);

            given(tokenProvider.createAccessToken(eq(1L), eq("google@test.com"), any(), any()))
                    .willReturn("access");
            given(tokenProvider.createRefreshToken(eq(1L), eq("google@test.com"), any(), any()))
                    .willReturn("refresh");

            OAuthSignupResponse response = sut.signupWithOAuth(cmd);

            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");

            then(memberAccountValidator).should().ensureUniqueUsername("testuser");
            then(memberAccountValidator).should().ensureUniqueEmail("google@test.com");

            then(authIdentityRepository).should().save(any());

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

            given(authIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.GITHUB, "gh123"))
                    .willReturn(Optional.empty());

            sut.linkOAuthAccount(registerToken, memberId);

            then(authIdentityRepository).should().save(any());
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

            given(authIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.GITHUB, "gh123"))
                    .willReturn(Optional.of(mock(AuthenticationIdentity.class)));

            assertThatThrownBy(() -> sut.linkOAuthAccount(registerToken, memberId))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }
}
