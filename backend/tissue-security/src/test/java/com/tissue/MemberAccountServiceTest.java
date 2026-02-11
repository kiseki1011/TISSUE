package com.tissue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.application.service.MemberAccountService;
import com.tissue.application.service.MemberEmailVerificationService;
import com.tissue.feature.member.application.port.out.AuthIdentityRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.MemberValidator;
import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
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
    AuthIdentityRepository authIdentityRepository;

    @Mock
    MemberValidator memberValidator;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @InjectMocks
    MemberAccountService sut;

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
}
