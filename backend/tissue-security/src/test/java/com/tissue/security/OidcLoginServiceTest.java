package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.OidcLoginResult;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.port.oidc.OidcClient;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.port.oidc.OidcTokenResult;
import com.tissue.security.application.service.OidcLoginService;
import com.tissue.security.application.service.OidcMemberResolver;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcLoginServiceTest {

    private static final OidcUserInfo USER = new OidcUserInfo("sub-1", "gildong@company.com", "gildong", "Gildong");

    @Mock
    private OidcClient oidcClient;

    @Mock
    private OidcMemberResolver oidcMemberResolver;

    @Mock
    private TokenPairCreateService tokenPairCreateService;

    private OidcLoginService service;

    @BeforeEach
    void setUp() {
        service = new OidcLoginService(oidcClient, oidcMemberResolver, tokenPairCreateService);
    }

    @Test
    @DisplayName("start: delegates to the OIDC client")
    void startDelegates() {
        OidcDeviceAuthorization auth =
                new OidcDeviceAuthorization("dc", "WDJB-MJHT", "https://idp/device", null, 5, 600);
        when(oidcClient.startDeviceAuthorization()).thenReturn(auth);

        assertThat(service.startDeviceLogin()).isSameAs(auth);
    }

    @Test
    @DisplayName("pending: a non authorized poll returns the status with no tokens")
    void pollPending() {
        when(oidcClient.pollToken("dc")).thenReturn(OidcTokenResult.of(OidcTokenResult.Status.PENDING));

        OidcLoginResult result = service.completeDeviceLogin("dc");

        assertThat(result.status()).isEqualTo(OidcTokenResult.Status.PENDING);
        assertThat(result.tokens()).isNull();
        verify(tokenPairCreateService, never()).createTokens(any(), any(), any(), any());
    }

    @Test
    @DisplayName("success: a completed poll resolves the member and issues Tissue tokens")
    void pollComplete() {
        Member member = Member.create("gildong@company.com", "gildong", "Gildong");
        when(oidcClient.pollToken("dc")).thenReturn(OidcTokenResult.complete(USER));
        when(oidcMemberResolver.resolve(USER)).thenReturn(member);
        when(tokenPairCreateService.createTokens(any(), any(), any(), any()))
                .thenReturn(new TokenPair("access-token", "refresh-token"));

        OidcLoginResult result = service.completeDeviceLogin("dc");

        assertThat(result.status()).isEqualTo(OidcTokenResult.Status.COMPLETE);
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("fail: a non-active member is rejected even after a successful IdP login")
    void rejectInactiveMember() {
        Member member = Member.create("gildong@company.com", "gildong", "Gildong");
        member.lock();
        when(oidcClient.pollToken("dc")).thenReturn(OidcTokenResult.complete(USER));
        when(oidcMemberResolver.resolve(USER)).thenReturn(member);

        assertThatThrownBy(() -> service.completeDeviceLogin("dc")).isInstanceOf(ForbiddenException.class);
        verify(tokenPairCreateService, never()).createTokens(any(), any(), any(), any());
    }
}
