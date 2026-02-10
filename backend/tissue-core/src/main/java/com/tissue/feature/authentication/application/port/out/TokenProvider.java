package com.tissue.feature.authentication.application.port.out;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public interface TokenProvider {

    // TODO: 별도의 TokenClaimKeys 로 분리할까?
    String CLAIM_TOKEN_TYPE = "tokenType";
    String CLAIM_MEMBER_ID = "memberId";
    String CLAIM_ELEVATED = "elevated";
    String CLAIM_PROVIDER = "provider";
    String CLAIM_IDENTIFIER = "identifier";
    String CLAIM_EMAIL = "email";
    String CLAIM_USERNAME = "username";
    String CLAIM_JTI = "jti";
    String CLAIM_AUTHORITIES = "authorities";

    String createAccessToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String createRefreshToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String createElevatedToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String getSubjectFromToken(String token);

    void validateRefreshToken(String token);

    long getRefreshTokenValidityInSeconds();

    // TODO: RegistrationTokenProvider로 인터페이스를 분리하는걸 권장
    String createRegisterToken(String provider, String identifier, String email);

    TokenClaims validateRegisterToken(String token);
}
