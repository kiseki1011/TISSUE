package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.global.setup.GlobalDefaultSetupService;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.MemberAccountValidator;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.application.service.MemberSignupService;
import com.tissue.security.application.service.SignupGuardrails;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    SignupGuardrails signupGuardrails;

    @Mock
    TissueSecurityProperties tissueSecurityProperties;

    @Mock
    GlobalDefaultSetupService globalDefaultSetupService;

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
        @DisplayName("success: the first user to sign up is promoted to SUPER_ADMIN")
        void firstUserBecomesSuperAdmin() {
            // given
            given(tissueSecurityProperties.isEmailRequired()).willReturn(true);
            given(signupGuardrails.isFirstUser()).willReturn(true);

            SignupMemberCommand cmd = SignupMemberCommand.builder()
                    .email("first@tissue.com")
                    .verifiedToken("validToken")
                    .username("first")
                    .password("password")
                    .name("First")
                    .build();

            given(memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verifiedToken()))
                    .willReturn(true);

            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            given(memberCommandRepository.save(memberCaptor.capture())).willReturn(savedMember);
            given(passwordEncoder.encode(cmd.password())).willReturn("encodedPassword");

            // when
            sut.signup(cmd);

            // then
            assertThat(memberCaptor.getValue().getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
            then(globalDefaultSetupService).should().setupDefaults();
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
}
