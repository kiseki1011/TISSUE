package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.global.setup.GlobalDefaultSetupService;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.OidcMemberResolver;
import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.exception.base.ForbiddenException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class OidcMemberResolverTest {

    private static final OidcUserInfo USER = new OidcUserInfo("sub-123", "gildong@company.com", "gildong", "Gildong");

    @Mock
    private AuthenticationIdentityRepository identityRepository;

    @Mock
    private MemberCommandRepository memberCommandRepository;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private GlobalDefaultSetupService globalDefaultSetupService;

    private TissueAuthProperties authProperties;
    private OidcMemberResolver resolver;

    @BeforeEach
    void setUp() {
        authProperties = new TissueAuthProperties();
        resolver = new OidcMemberResolver(
                identityRepository,
                memberCommandRepository,
                memberQueryRepository,
                globalDefaultSetupService,
                authProperties);
    }

    @Test
    @DisplayName("success: existing OIDC identity returns its member without provisioning")
    void existingIdentity() {
        Member existing = Member.create("gildong@company.com", "gildong", "Gildong");
        AuthenticationIdentity identity = AuthenticationIdentity.createOidcIdentity(existing, "sub-123");
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.of(identity));

        Member result = resolver.resolve(USER);

        assertThat(result).isSameAs(existing);
        verify(memberCommandRepository, never()).save(any());
        verify(globalDefaultSetupService, never()).setupDefaults();
    }

    @Test
    @DisplayName("success: unknown subject provisions a new USER member (not first user)")
    void provisionRegularUser() {
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty());
        when(memberQueryRepository.count()).thenReturn(5L);
        when(memberCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = resolver.resolve(USER);

        assertThat(result.getRole()).isEqualTo(SystemRole.USER);
        assertThat(result.getEmail()).isEqualTo("gildong@company.com");
        assertThat(result.getUsername()).isEqualTo("gildong");
        verify(identityRepository).save(any(AuthenticationIdentity.class));
        verify(globalDefaultSetupService, never()).setupDefaults();
    }

    @Test
    @DisplayName("success: first user is provisioned as SUPER_ADMIN and triggers default setup")
    void provisionFirstUserAsSuperAdmin() {
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty());
        when(memberQueryRepository.count()).thenReturn(0L);
        when(memberCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = resolver.resolve(USER);

        assertThat(result.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        verify(globalDefaultSetupService).setupDefaults();
    }

    @Test
    @DisplayName("success: username is derived from the email local-part when the IdP omits it")
    void usernameDerivedFromEmailLocalPart() {
        OidcUserInfo noUsername = new OidcUserInfo("sub-derive", "alice@company.com", null, "Alice");
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-derive"))
                .thenReturn(Optional.empty());
        when(memberQueryRepository.count()).thenReturn(4L);
        when(memberCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = resolver.resolve(noUsername);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("fail: provisioning disabled throws ForbiddenException")
    void provisioningDisabled() {
        authProperties.getOidc().setAutoProvision(false);
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(USER)).isInstanceOf(ForbiddenException.class);
        verify(memberCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("fail: OIDC without an email is rejected (email is mandatory in OIDC mode)")
    void emailMissingRejected() {
        OidcUserInfo noEmail = new OidcUserInfo("sub-999", null, "nobody", "No Body");
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(noEmail)).isInstanceOf(ForbiddenException.class);
        verify(memberCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("fail: email domain not in allow-list throws ForbiddenException")
    void domainNotAllowed() {
        authProperties.getOidc().setAllowedEmailDomains(List.of("allowed.com"));
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(USER)).isInstanceOf(ForbiddenException.class);
        verify(memberCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("success: email domain in allow-list provisions the member")
    void domainAllowed() {
        authProperties.getOidc().setAllowedEmailDomains(List.of("company.com"));
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty());
        when(memberQueryRepository.count()).thenReturn(3L);
        when(memberCommandRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = resolver.resolve(USER);

        assertThat(result.getUsername()).isEqualTo("gildong");
        verify(identityRepository).save(any(AuthenticationIdentity.class));
    }

    @Test
    @DisplayName("success: concurrent provisioning conflict falls back to re-reading the identity")
    void concurrentProvisionConflict() {
        Member existing = Member.create("gildong@company.com", "gildong", "Gildong");
        AuthenticationIdentity identity = AuthenticationIdentity.createOidcIdentity(existing, "sub-123");
        when(identityRepository.findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, "sub-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(identity));
        when(memberQueryRepository.count()).thenReturn(2L);
        when(memberCommandRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        Member result = resolver.resolve(USER);

        assertThat(result).isSameAs(existing);
    }
}
