package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.service.MemberAccountService;
import com.tissue.security.application.service.MemberAccountValidator;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.List;
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
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    MemberAccountValidator memberAccountValidator;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @Mock
    TissueSecurityProperties tissueSecurityProperties;

    @InjectMocks
    MemberAccountService sut;

    @Nested
    @DisplayName("update username")
    class UpdateUsername {

        @Test
        @DisplayName("success: updates username")
        void success_UpdateUsername() {
            // given
            Long memberId = 1L;
            String newUsername = "newUserName";

            Member member = mock(Member.class);
            given(memberFinder.getActiveById(memberId)).willReturn(member);

            // when
            sut.updateUsername(newUsername, memberId);

            // then
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
            // given
            Long memberId = 1L;
            String newEmail = "new@tissue.com";
            String token = "validToken";

            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("old@tissue.com");
            given(memberFinder.getActiveById(memberId)).willReturn(member);
            given(memberEmailVerificationService.isTokenVerified(newEmail, token))
                    .willReturn(true);

            // when
            sut.updateEmail(newEmail, token, memberId);

            // then
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
            // given
            Long memberId = 1L;
            String oldPass = "oldPassword";
            String newPass = "newPassword";

            given(tissueSecurityProperties.isEmailRequired()).willReturn(true);
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveById(memberId)).willReturn(member);
            given(passwordEncoder.encode(newPass)).willReturn("encodedNewPassword");

            AuthenticationIdentity emailIdentity = mock(AuthenticationIdentity.class);
            AuthenticationIdentity usernameIdentity = mock(AuthenticationIdentity.class);
            given(authenticationIdentityRepository.findAllByMemberIdAndProviderIn(
                            memberId,
                            List.of(AuthenticationIdentityProvider.EMAIL, AuthenticationIdentityProvider.USERNAME)))
                    .willReturn(List.of(emailIdentity, usernameIdentity));

            // when
            sut.updatePassword(oldPass, newPass, memberId);

            // then
            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(emailIdentity).should().updateCredential("encodedNewPassword");
            then(usernameIdentity).should().updateCredential("encodedNewPassword");
            then(refreshTokenRepository).should().deleteByMemberId(memberId);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("success: authenticates, checks withdrawable, and withdraws")
        void success_Withdraw() {
            // given
            Long memberId = 1L;
            String password = "password";

            given(tissueSecurityProperties.isEmailRequired()).willReturn(true);
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveById(memberId)).willReturn(member);

            // when
            sut.withdraw(password, memberId);

            // then
            then(authenticationManager).should().authenticate(any());
            then(memberAccountValidator).should().ensureWithdrawable(member);
            then(member).should().withdraw();
            then(refreshTokenRepository).should().deleteByMemberId(memberId);
        }
    }

    @Nested
    @DisplayName("link email account")
    class LinkEmail {

        @Test
        @DisplayName("success: adds password identity")
        void success_LinkEmail() {
            // given
            Long memberId = 1L;
            String newPassword = "newPassword";
            Member member = mock(Member.class);

            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveById(memberId)).willReturn(member);

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.empty());

            given(passwordEncoder.encode(newPassword)).willReturn("encoded");

            // when
            sut.linkEmailAuthentication(newPassword, memberId);

            // then
            then(authenticationIdentityRepository).should().save(any(AuthenticationIdentity.class));
        }

        @Test
        @DisplayName("fail: password identity already exists")
        void fail_AlreadyExists() {
            // given
            Long memberId = 1L;
            Member member = mock(Member.class);

            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveById(memberId)).willReturn(member);

            given(authenticationIdentityRepository.findByProviderAndIdentifier(
                            AuthenticationIdentityProvider.EMAIL, "test@tissue.com"))
                    .willReturn(Optional.of(mock(AuthenticationIdentity.class)));

            // when & then
            assertThatThrownBy(() -> sut.linkEmailAuthentication("pass", memberId))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }
}
