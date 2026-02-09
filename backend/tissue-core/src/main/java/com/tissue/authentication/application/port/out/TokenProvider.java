package com.tissue.authentication.application.port.out;

import io.jsonwebtoken.Claims;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public interface TokenProvider {

    String CLAIM_TOKEN_TYPE = "tokenType";
    String CLAIM_MEMBER_ID = "memberId";
    String CLAIM_ELEVATED = "elevated";
    String CLAIM_PROVIDER = "provider";
    String CLAIM_IDENTIFIER = "identifier";
    String CLAIM_EMAIL = "email";
    String CLAIM_JTI = "jti";
    String CLAIM_AUTHORITIES = "authorities";

    String createAccessToken(Long memberId, String email, Collection<? extends GrantedAuthority> authorities);

    String createRefreshToken(Long memberId, String email, Collection<? extends GrantedAuthority> authorities);

    String createElevatedToken(Long memberId, String email, Collection<? extends GrantedAuthority> authorities);

    String createRegisterToken(String provider, String identifier, String email);

    Claims validateRegisterToken(String token);

    Authentication getAuthentication(String token);

    String getSubjectFromToken(String token);

    boolean getElevatedFromToken(String token);

    void validateAccessToken(String token);

    void validateRefreshToken(String token);

    long getRefreshTokenValidityInSeconds();
}
