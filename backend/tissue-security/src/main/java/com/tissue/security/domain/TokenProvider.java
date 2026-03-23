package com.tissue.security.domain;

import java.time.Duration;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public interface TokenProvider {

    String CLAIM_TOKEN_TYPE = "tokenType";
    String CLAIM_MEMBER_ID = "memberId";
    String CLAIM_ELEVATED = "elevated";
    String CLAIM_PROVIDER = "provider";
    String CLAIM_IDENTIFIER = "identifier";
    String CLAIM_EMAIL = "email";
    String CLAIM_NAME = "name";
    String CLAIM_USERNAME = "username";
    String CLAIM_JTI = "jti";
    String CLAIM_AUTHORITIES = "authorities";

    String createAccessToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String createRefreshToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String createElevatedToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities);

    String validateRefreshTokenAndGetSubject(String token);

    Duration getRefreshTokenValidity();

    String createRegisterToken(String provider, String identifier, String email);

    TokenClaims validateRegisterToken(String token);
}
