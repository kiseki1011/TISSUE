package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.MemberAccountValidator;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.application.service.MemberSignupService;
import com.tissue.security.application.service.SignupGuardrails;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class MemberSignupServiceTest {

    @Mock
    MemberFinder memberFinder;

    @Mock
    MemberCommandRepository memberCommandRepository;

    @Mock
    AuthenticationIdentityRepository authenticationIdentityRepository;

    @Mock
    MemberAccountValidator memberAccountValidator;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @Mock
    TokenProvider tokenProvider;

    @Mock
    TokenPairCreateService tokenPairCreateService;

    @Mock
    SignupGuardrails signupGuardrails;

    @Mock
    TissueSecurityProperties tissueSecurityProperties;

    @InjectMocks
    MemberSignupService sut;

    @Nested
    @DisplayName("signup with email")
    class Signup {

        @Test
        @DisplayName("success: creates member and identity, consumes verification token")
        void successSignup() {
            // given
            given(tissueSecurityProperties.isEmailRequired()).willReturn(true);

            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .email("test@tissue.com")
                    .verifiedToken("validToken")
                    .username("testuser")
                    .password("password")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verifiedToken()))
                    .willReturn(true);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);
            given(passwordEncoder.encode(cmd.password())).willReturn("encodedPassword");

            // when
            MemberSignupResponse response = sut.signup(cmd);

            // then
            assertThat(response.memberId()).isEqualTo(1L);
            then(memberAccountValidator).should().ensureUniqueEmail(cmd.email());
            then(memberAccountValidator).should().ensureUniqueUsername(cmd.username());

            then(authenticationIdentityRepository).should(times(2)).save(any());
        }

        @Test
        @DisplayName("fail: if signup token is invalid, throws EmailNotVerifiedException")
        void failSignup_If_TokenInvalid() {
            // given
            given(tissueSecurityProperties.isEmailRequired()).willReturn(true);

            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .email("test@tissue.com")
                    .verifiedToken("invalidToken")
                    .username("testuser")
                    .password("password")
                    .name("name")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verifiedToken()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(EmailNotVerifiedException.class);
            then(memberCommandRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("signup with OAuth")
    class SignupOAuth {

        @Test
        @DisplayName("success: creates member and identity, returns login tokens")
        void success() {
            // given
            String registerToken = "regToken";
            SignupOAuthMemberCommand cmd = new SignupOAuthMemberCommand(registerToken, "testuser", "testname");

            TokenClaims claims = TokenClaims.builder()
                    .provider("GOOGLE")
                    .identifier("sub123")
                    .email("google@test.com")
                    .build();
            given(tokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(savedMember.getEmail()).willReturn("google@test.com");
            given(savedMember.getUsername()).willReturn("testuser");
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);
            given(savedMember.getRole()).willReturn(SystemRole.USER);

            given(tokenPairCreateService.createTokens(eq(1L), eq("google@test.com"), eq("testuser"), any()))
                    .willReturn(new TokenPair("access", "refresh"));

            // when
            OAuthSignupResponse response = sut.signupWithOAuth(cmd);

            // then
            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");

            then(memberAccountValidator).should().ensureUniqueUsername("testuser");
            then(memberAccountValidator).should().ensureUniqueEmail("google@test.com");

            then(authenticationIdentityRepository).should().save(any(AuthenticationIdentity.class));

            then(tokenPairCreateService).should().createTokens(eq(1L), eq("google@test.com"), eq("testuser"), any());
        }

        @Test
        @DisplayName("success: handles lowercase provider from register token")
        void success_LowercaseProvider() {
            // given
            String registerToken = "regToken";
            SignupOAuthMemberCommand cmd = new SignupOAuthMemberCommand(registerToken, "testuser", "testname");

            TokenClaims claims = TokenClaims.builder()
                    .provider("google")
                    .identifier("sub123")
                    .email("google@test.com")
                    .build();
            given(tokenProvider.validateRegisterToken(registerToken)).willReturn(claims);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(savedMember.getEmail()).willReturn("google@test.com");
            given(savedMember.getUsername()).willReturn("testuser");
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);
            given(savedMember.getRole()).willReturn(SystemRole.USER);

            given(tokenPairCreateService.createTokens(eq(1L), eq("google@test.com"), eq("testuser"), any()))
                    .willReturn(new TokenPair("access", "refresh"));

            // when
            OAuthSignupResponse response = sut.signupWithOAuth(cmd);

            // then
            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");

            then(authenticationIdentityRepository).should().save(any(AuthenticationIdentity.class));
        }
    }
}
