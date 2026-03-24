package com.tissue.security.application.service;

import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.domain.TokenProvider;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenPairCreateService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenPair createTokens(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        String accessToken = tokenProvider.createAccessToken(memberId, email, username, authorities);
        String refreshToken = tokenProvider.createRefreshToken(memberId, email, username, authorities);

        refreshTokenRepository.save(email, refreshToken, tokenProvider.getRefreshTokenValidity());

        return new TokenPair(accessToken, refreshToken);
    }
}
