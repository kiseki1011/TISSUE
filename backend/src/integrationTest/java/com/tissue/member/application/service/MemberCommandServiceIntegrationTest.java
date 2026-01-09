package com.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.member.adapter.out.persistence.RedisEmailVerificationRepository;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.security.authentication.application.port.out.TokenProvider;
import com.tissue.security.authentication.presentation.dto.response.OAuthSignupResponse;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberCommandService memberCommandService;

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

    /**
     * <li>{@code RedisEmailVerificationRepository#saveToken(email, val)}
     * saves "email_verification:{email_value}" as key</li>
     * <li>{@code checkVerifiedToken(email, token)} checks if value is "{token_value}:verified"</li>
     * <br>
     * Look at {@link RedisEmailVerificationRepository}
     */
    @Test
    @DisplayName("Standard signup creates member and identity (email strategy: redis)")
    void signupSuccess() {
        // given
        String email = "signup@test.com";
        String token = "valid-token-value";

        emailVerificationRepository.saveToken(email, token + ":verified", Duration.ofMinutes(10));

        // token verification returns true (token should be verified before signup)
        boolean verified = emailVerificationRepository.checkVerifiedToken(email, token);
        assertThat(verified).isTrue();

        // TODO: add reserved usenames like "user", "admin", "tester", "test", etc...
        SignupMemberCommand command =
                new SignupMemberCommand(AuthProvider.EMAIL, email, token, "signupuser", "password123", "name");

        // when
        MemberSignupResponse response = memberCommandService.signup(command);

        // then
        assertThat(response).isNotNull();

        Member savedMember = memberQueryRepository.findById(response.memberId()).orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getUsername()).isEqualTo("signupuser");
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.EMAIL, email))
                .isPresent();
    }

    @Test
    @DisplayName("OAuth signup with valid register token creates member")
    void signupOAuthSuccess() {
        // given
        String email = "oauth@test.com";
        String providerId = "google-123";
        String registerToken = tokenProvider.createRegisterToken(AuthProvider.GOOGLE.name(), providerId, email);

        SignupOAuthMemberCommand command = new SignupOAuthMemberCommand(registerToken, "oauthuser", "oauthuser name");

        // when
        OAuthSignupResponse response = memberCommandService.signupOAuth(command);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(memberQueryRepository.findByEmail(email)).isPresent();
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.GOOGLE, providerId))
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
                tokenProvider.createRegisterToken(AuthProvider.GITHUB.name(), providerId, "link@test.com");

        // when
        memberCommandService.linkOAuthAccount(registerToken, member.getId());

        // then
        assertThat(authIdentityRepository.findByProviderAndIdentifier(AuthProvider.GITHUB, providerId))
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
        AuthIdentity existingIdentity = AuthIdentity.createSocialIdentity(member, AuthProvider.GITHUB, providerId);
        authIdentityRepository.save(existingIdentity);
        String registerToken =
                tokenProvider.createRegisterToken(AuthProvider.GITHUB.name(), providerId, "duplicate@test.com");

        // when & then
        assertThatThrownBy(() -> memberCommandService.linkOAuthAccount(registerToken, member.getId()))
                .isInstanceOf(com.tissue.global.exception.base.ResourceConflictException.class);
    }
}
