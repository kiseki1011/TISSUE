package com.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberExceptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock
    MemberFinder memberFinder;

    @Mock
    MemberCommandRepository memberCommandRepository;

    @Mock
    MemberValidator memberValidator;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MemberEmailVerificationService memberEmailVerificationService;

    @InjectMocks
    MemberCommandService sut;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("success: creates member and clears verification")
        void success_Signup() {
            SignupMemberCommand cmd = new SignupMemberCommand("test@tissue.com", "user1", "pass123", "User One");
            given(memberEmailVerificationService.isEmailVerified(cmd.email())).willReturn(true);
            given(passwordEncoder.encode(cmd.password())).willReturn("encodedPass");

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(memberCommandRepository.save(any(Member.class))).willReturn(savedMember);

            MemberSignupResponse response = sut.signup(cmd);

            assertThat(response.memberId()).isEqualTo(1L);
            then(memberValidator).should().ensureUniqueEmail(cmd.email());
            then(memberValidator).should().ensureUniqueUsername(cmd.username());
            then(memberEmailVerificationService).should().clearVerification(cmd.email());
        }

        @Test
        @DisplayName("fail: email not verified")
        void fail_EmailNotVerified() {
            SignupMemberCommand cmd = new SignupMemberCommand("test@tissue.com", "user1", "pass123", "User One");
            given(memberEmailVerificationService.isEmailVerified(cmd.email())).willReturn(false);

            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(ForbiddenException.class);
            then(memberCommandRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fail: duplicate (DataIntegrityViolation)")
        void fail_DuplicationConflict() {
            SignupMemberCommand cmd = new SignupMemberCommand("test@tissue.com", "user1", "pass123", "User One");
            given(memberEmailVerificationService.isEmailVerified(cmd.email())).willReturn(true);
            given(memberCommandRepository.save(any(Member.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate"));

            assertThatThrownBy(() -> sut.signup(cmd)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Test
    @DisplayName("success: updates name")
    void success_UpdateName() {
        Long memberId = 1L;
        String newName = "New Name";
        Member member = mock(Member.class);
        given(memberFinder.getActiveBy(memberId)).willReturn(member);

        sut.updateName(newName, memberId);

        then(member).should().updateName(newName);
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
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(memberEmailVerificationService.isEmailVerified(newEmail)).willReturn(true);

            sut.updateEmail(newEmail, memberId);

            then(memberValidator).should().ensureUniqueEmail(newEmail);
            then(member).should().updateEmail(newEmail);
            then(memberEmailVerificationService).should().clearVerification(newEmail);
        }

        @Test
        @DisplayName("fail: email not verified")
        void fail_NotVerified() {
            Long memberId = 1L;
            String newEmail = "new@tissue.com";
            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(memberEmailVerificationService.isEmailVerified(newEmail)).willReturn(false);

            assertThatThrownBy(() -> sut.updateEmail(newEmail, memberId)).isInstanceOf(ForbiddenException.class);
            then(member).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("success: updates username")
    void success_UpdateUsername() {
        Long memberId = 1L;
        String newUsername = "newUser";
        Member member = mock(Member.class);
        given(memberFinder.getActiveBy(memberId)).willReturn(member);

        sut.updateUsername(newUsername, memberId);

        then(memberValidator).should().ensureUniqueUsername(newUsername);
        then(member).should().updateUsername(newUsername);
    }

    @Nested
    @DisplayName("update password")
    class UpdatePassword {
        @Test
        @DisplayName("success: authenticates and updates password")
        void success_UpdatePassword() {
            Long memberId = 1L;
            String oldPass = "oldPass";
            String newPass = "newPass";
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(passwordEncoder.encode(newPass)).willReturn("encodedNewPass");

            sut.updatePassword(oldPass, newPass, memberId);

            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(member).should().updatePassword("encodedNewPass");
        }

        @Test
        @DisplayName("fail: authentication failed")
        void fail_AuthFailed() {
            Long memberId = 1L;
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            willThrow(new BadCredentialsException("Bad creds"))
                    .given(authenticationManager)
                    .authenticate(any());

            assertThatThrownBy(() -> sut.updatePassword("wrong", "new", memberId))
                    .isInstanceOf(BadCredentialsException.class);
            then(member).should(org.mockito.Mockito.never()).updatePassword(any());
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {
        @Test
        @DisplayName("success: authenticates, checks withdrawable, and withdraws")
        void success_Withdraw() {
            Long memberId = 1L;
            String password = "pass";
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            sut.withdraw(password, memberId);

            then(authenticationManager).should().authenticate(any());
            then(memberValidator).should().ensureWithdrawable(member);
            then(member).should().withdraw();
        }

        @Test
        @DisplayName("fail: member validator throws exception if OWNER of a workspace")
        void fail_NotWithdrawable() {
            Long memberId = 1L;
            Member member = mock(Member.class);
            given(member.getEmail()).willReturn("test@tissue.com");
            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            willThrow(MemberExceptions.ownerNotWithdrawable(member))
                    .given(memberValidator)
                    .ensureWithdrawable(member);

            assertThatThrownBy(() -> sut.withdraw("pass", memberId)).isInstanceOf(BadRequestException.class);
            then(member).should(Mockito.never()).withdraw();
        }
    }
}
