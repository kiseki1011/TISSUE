package com.tissue.security.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.application.service.MemberSignupService;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.TokenProvider;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.EmailVerificationProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberSignupServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberSignupService sut;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationProperties emailVerificationProperties;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("Standard signup creates member and identity (secure flow)")
    void signupWithEmailSuccess() {
        // given
        String email = "signup@test.com";
        String emailToken = "secure-email-token";

        String verificationId =
                emailVerificationRepository.startVerification(email, emailToken, emailVerificationProperties.getTtl());

        boolean verifyResult =
                emailVerificationRepository.verifyByToken(emailToken, emailVerificationProperties.getSignupTokenTtl());
        assertThat(verifyResult).isTrue();

        // get secure signup token (polling)
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
        MemberSignupResponse response = sut.signupWithEmail(command);

        // then
        assertThat(response).isNotNull();

        Member savedMember = memberQueryRepository.findById(response.memberId()).orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getUsername()).isEqualTo("signupuser");
        assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(AuthenticationProvider.EMAIL, email))
                .isPresent();

        // ensure signup token is consumed
        assertThat(emailVerificationRepository.validateSignupToken(email, signupToken))
                .isFalse();
    }

    @Test
    @DisplayName("OAuth signup with valid register token creates member")
    void signupWithEmailOAuthSuccess() {
        // given
        String email = "oauth@test.com";
        String providerId = "google-123";
        String registerToken =
                tokenProvider.createRegisterToken(AuthenticationProvider.GOOGLE.name(), providerId, email);

        SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "oauthuser name");

        // when
        OAuthSignupResponse response = sut.signupWithOAuth(command);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(memberQueryRepository.findByEmail(email)).isPresent();
        assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                        AuthenticationProvider.GOOGLE, providerId))
                .isPresent();
    }
}
