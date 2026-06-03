package com.tissue.security.jwt;

import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.TokenType;
import com.tissue.security.domain.exception.TokenExpiredException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider implements TokenProvider {

    public static final int SECRET_KEY_LENGTH = 32;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(
            TissueSecurityProperties tissueSecurityProperties, JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        TissueSecurityProperties.Jwt jwt = tissueSecurityProperties.getJwt();
        String secret = jwt.getSecret();

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < SECRET_KEY_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least " + SECRET_KEY_LENGTH + " bytes (UTF-8).");
        }

        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenValidity = jwt.getAccessTokenValidity();
        this.refreshTokenValidity = jwt.getRefreshTokenValidity();
    }

    @Override
    public String createAccessToken(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return createToken(memberId, TokenType.ACCESS, accessTokenValidity, email, username, authorities);
    }

    @Override
    public String createRefreshToken(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return createToken(memberId, TokenType.REFRESH, refreshTokenValidity, email, username, authorities);
    }

    @Override
    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    @Override
    public Long validateRefreshTokenAndGetMemberId(String token) {
        Jwt jwt = decode(token);

        if (!Objects.equals(TokenType.REFRESH.getValue(), jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new JwtTokenException();
        }

        return Long.parseLong(jwt.getSubject());
    }

    private String createToken(
            Long memberId,
            TokenType tokenType,
            Duration validity,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        List<String> roles =
                authorities.stream().map(GrantedAuthority::getAuthority).toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(String.valueOf(memberId))
                .issuer(TokenProvider.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(validity))
                .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                .claim(CLAIM_MEMBER_ID, memberId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_AUTHORITIES, roles);

        if (email != null) {
            claims.claim(CLAIM_EMAIL, email);
        }
        if (tokenType == TokenType.REFRESH) {
            claims.id(UUID.randomUUID().toString());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        try {
            return jwtEncoder
                    .encode(JwtEncoderParameters.from(header, claims.build()))
                    .getTokenValue();
        } catch (JwtException e) {
            throw new JwtTokenException();
        }
    }

    private Jwt decode(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtValidationException e) {
            boolean expired = e.getErrors().stream()
                    .anyMatch(error -> error.getDescription() != null
                            && error.getDescription().toLowerCase().contains("expired"));
            if (expired) {
                throw new TokenExpiredException();
            }
            throw new JwtTokenException();
        } catch (JwtException e) {
            throw new JwtTokenException();
        }
    }
}
