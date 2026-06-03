package com.tissue.security.application.service;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.security.application.dto.OidcLoginResult;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.port.oidc.OidcClient;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.port.oidc.OidcTokenResult;
import com.tissue.shared.exception.base.ForbiddenException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Brokers OIDC device flow login for the (TUI) client.
 *
 * <p>Relays the device handshake to the IdP, and on
 * success maps the IdP identity to a Tissue {@link Member} and issues Tissue's own tokens.
 */
@Service
@ConditionalOnProperty(name = "tissue.auth.mode", havingValue = "OIDC")
@RequiredArgsConstructor
public class OidcLoginService {

    private final OidcClient oidcClient;
    private final OidcMemberResolver oidcMemberResolver;
    private final TokenPairCreateService tokenPairCreateService;

    public OidcDeviceAuthorization startDeviceLogin() {
        return oidcClient.startDeviceAuthorization();
    }

    public OidcLoginResult completeDeviceLogin(String deviceCode) {
        OidcTokenResult result = oidcClient.pollToken(deviceCode);
        if (result.status() != OidcTokenResult.Status.COMPLETE) {
            return OidcLoginResult.of(result.status());
        }

        OidcUserInfo userInfo = Objects.requireNonNull(result.userInfo());
        Member member = oidcMemberResolver.resolve(userInfo);
        ensureMemberActive(member);

        TokenPair tokens = tokenPairCreateService.createTokens(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())));

        return OidcLoginResult.complete(tokens);
    }

    private void ensureMemberActive(Member member) {
        if (!member.isActive()) {
            throw new ForbiddenException(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
    }
}
