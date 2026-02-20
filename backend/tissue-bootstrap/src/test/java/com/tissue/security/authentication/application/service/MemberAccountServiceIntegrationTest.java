package com.tissue.security.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.service.MemberAccountService;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.TokenProvider;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MemberAccountServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberAccountService sut;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("Linking OAuth account to existing member works")
    void linkOAuthAccountSuccess() {
        // given
        Member member = Member.create("link@test.com", "linkuser", "LinkUser");
        memberCommandRepository.save(member);
        String providerId = "github-456";
        String registerToken =
                tokenProvider.createRegisterToken(AuthenticationProvider.GITHUB.name(), providerId, "link@test.com");

        // when
        sut.linkOAuthAccount(registerToken, member.getId());

        // then
        assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                        AuthenticationProvider.GITHUB, providerId))
                .isPresent();
    }

    @Test
    @DisplayName("Linking existing OAuth account throws exception")
    void linkOAuthAccountDuplicate() {
        // given
        Member member = Member.create("duplicate@test.com", "dupuser", "DupUser");
        memberCommandRepository.save(member);

        // create existing identity
        String providerId = "github-789";
        AuthenticationIdentity existingIdentity =
                AuthenticationIdentity.createSocialIdentity(member, AuthenticationProvider.GITHUB, providerId);
        authenticationIdentityRepository.save(existingIdentity);
        String registerToken = tokenProvider.createRegisterToken(
                AuthenticationProvider.GITHUB.name(), providerId, "duplicate@test.com");

        // when & then
        assertThatThrownBy(() -> sut.linkOAuthAccount(registerToken, member.getId()))
                .isInstanceOf(ResourceConflictException.class);
    }
}
