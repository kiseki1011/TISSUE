package com.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.repository.AuthIdentityRepository;
import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.application.service.MemberSignupService;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.TokenProvider;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberSignupServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberSignupService sut;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthIdentityRepository authIdentityRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("Standard signup creates member and identity (secure flow)")
    void signupSuccess() {
        // given
        String email = "signup@test.com";
        String emailToken = "secure-email-token";

        // start verification (TUI -> Backend)
        String verificationId =
                emailVerificationRepository.startVerification(email, emailToken, Duration.ofMinutes(10));

        // verify by email token (User -> Backend)
        boolean verifyResult = emailVerificationRepository.verifyByToken(emailToken);
        assertThat(verifyResult).isTrue();

        // get secure signup token (TUI Polling)
        var status = emailVerificationRepository.getStatus(verificationId);
        assertThat(status.status()).isEqualTo("VERIFIED");
        String signupToken = status.signupToken();
        assertThat(signupToken).isNotNull();

        // command includes secure signupToken
        SignupMemberCommand command = SignupMemberCommand.builder()
                .provider(AuthenticationProvider.EMAIL)
                .email(email)
                .signupToken(signupToken)
                .username("signupuser")
                .password("password123")
                .name("name")
                .build();

        // when
        MemberSignupResponse response = sut.signup(command);

        // then
        assertThat(response).isNotNull();

        Member savedMember = memberQueryRepository.findById(response.memberId()).orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getUsername()).isEqualTo("signupuser");
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.EMAIL, email))
                .isPresent();

        // ensure signup token is consumed
        assertThat(emailVerificationRepository.validateSignupToken(email, signupToken))
                .isFalse();
    }

    @Test
    @DisplayName("OAuth signup with valid register token creates member")
    void signupOAuthSuccess() {
        // given
        String email = "oauth@test.com";
        String providerId = "google-123";
        String registerToken =
                tokenProvider.createRegisterToken(AuthenticationProvider.GOOGLE.name(), providerId, email);

        SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "oauthuser name");

        // when
        OAuthSignupResponse response = sut.signupOAuth(command);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(memberQueryRepository.findByEmail(email)).isPresent();
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.GOOGLE, providerId))
                .isPresent();
    }

    @Test
    @DisplayName("Linking OAuth account to existing member works")
    void linkOAuthAccountSuccess() {
        // given
        Member member = Member.create("link@test.com", "linkuser", "linkuser name");
        memberCommandRepository.save(member);
        String providerId = "github-456";
        String registerToken =
                tokenProvider.createRegisterToken(AuthenticationProvider.GITHUB.name(), providerId, "link@test.com");

        // when
        sut.linkOAuthAccount(registerToken, member.getId());

        // then
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.GITHUB, providerId))
                .isPresent();
    }

    @Test
    @DisplayName("Linking existing OAuth account throws exception")
    void linkOAuthAccountDuplicate() {
        // given
        Member member = Member.create("duplicate@test.com", "dupuser", "dupuser name");
        memberCommandRepository.save(member);

        // create existing identity
        String providerId = "github-789";
        AuthenticationIdentity existingIdentity =
                AuthenticationIdentity.createSocialIdentity(member, AuthenticationProvider.GITHUB, providerId);
        authIdentityRepository.save(existingIdentity);
        String registerToken = tokenProvider.createRegisterToken(
                AuthenticationProvider.GITHUB.name(), providerId, "duplicate@test.com");

        // when & then
        assertThatThrownBy(() -> sut.linkOAuthAccount(registerToken, member.getId()))
                .isInstanceOf(ResourceConflictException.class);
    }
}
