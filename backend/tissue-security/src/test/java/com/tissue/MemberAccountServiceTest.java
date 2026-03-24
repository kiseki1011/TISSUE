package com.tissue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.MemberAccountService;
import com.tissue.security.application.service.MemberAccountValidator;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class MemberAccountServiceTest {

    @Mock
    MemberFinder memberFinder;

    @Mock
    AuthenticationIdentityRepository authenticationIdentityRepository;

    @Mock
    MemberAccountValidator memberAccountValidator;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @Mock
    TokenProvider tokenProvider;

    @InjectMocks
    MemberAccountService sut;

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

            then(memberAccountValidator).should().ensureUniqueUsername(newUsername);
            then(member).should().updateUsername(newUsername);
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
            given(memberEmailVerificationService.isTokenVerified(newEmail, token))
                    .willReturn(true);

            sut.updateEmail(newEmail, token, memberId);

            then(memberAccountValidator).should().ensureUniqueEmail(newEmail);
            then(member).should().updateEmail(newEmail);
            then(authenticationIdentityRepository)
                    .should()
                    .findByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, "old@tissue.com");
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

            AuthenticationIdentity authenticationIdentity = mock(AuthenticationIdentity.class);
            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.of(authenticationIdentity));

            sut.updatePassword(oldPass, newPass, memberId);

            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(authenticationIdentity).should().updateCredential("encodedNewPassword");
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
            then(memberAccountValidator).should().ensureWithdrawable(member);
            then(member).should().withdraw();
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

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.empty());

            given(passwordEncoder.encode(newPassword)).willReturn("encoded");

            sut.linkEmailAuthentication(newPassword, memberId);

            then(authenticationIdentityRepository).should().save(any(AuthenticationIdentity.class));
        }

        @Test
        @DisplayName("fail: password identity already exists")
        void fail_AlreadyExists() {
            Long memberId = 1L;
            Member member = mock(Member.class);

            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.of(mock(AuthenticationIdentity.class)));

            assertThatThrownBy(() -> sut.linkEmailAuthentication("pass", memberId))
                    .isInstanceOf(ResourceConflictException.class);
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

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.GITHUB, "gh123"))
                    .willReturn(Optional.empty());

            sut.linkOAuthAccount(registerToken, memberId);

            then(authenticationIdentityRepository).should().save(any());
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

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.GITHUB, "gh123"))
                    .willReturn(Optional.of(mock(AuthenticationIdentity.class)));

            assertThatThrownBy(() -> sut.linkOAuthAccount(registerToken, memberId))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }
}
