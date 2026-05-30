package com.tissue.security.domain;

import java.time.Duration;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

public interface TokenProvider {

    String ISSUER = "TISSUE";
    String CLAIM_TOKEN_TYPE = "tokenType";
    String CLAIM_MEMBER_ID = "memberId";
    String CLAIM_PROVIDER = "provider";
    String CLAIM_IDENTIFIER = "identifier";
    String CLAIM_EMAIL = "email";
    String CLAIM_USERNAME = "username";
    String CLAIM_AUTHORITIES = "authorities";

    String createAccessToken(
            Long memberId, @Nullable String email, String username, Collection<? extends GrantedAuthority> authorities);

    String createRefreshToken(
            Long memberId, @Nullable String email, String username, Collection<? extends GrantedAuthority> authorities);

    Long validateRefreshTokenAndGetMemberId(String token);

    Duration getRefreshTokenValidity();

    String createRegisterToken(String provider, String identifier, String email);

    TokenClaims validateRegisterToken(String token);
}
