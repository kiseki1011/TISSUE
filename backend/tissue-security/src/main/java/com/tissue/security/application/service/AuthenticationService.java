package com.tissue.security.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.port.usecase.AuthenticationUseCase;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.RefreshTokenNotFoundException;
import com.tissue.security.domain.exception.TokenReuseDetectedException;
import com.tissue.security.util.TokenHashUtil;
import com.tissue.shared.auth.MemberDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final TokenPairCreateService tokenPairCreateService;
    private final MemberFinder memberFinder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitService rateLimitService;

    @Override
    public LoginResponse login(String identifier, String password, String clientIp) {
        rateLimitService.checkLoginRateLimit(clientIp, identifier);

        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

        MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

        TokenPair tokens = tokenPairCreateService.createTokens(
                userDetails.getMemberId(),
                userDetails.getEmail(),
                userDetails.getUsername(),
                userDetails.getAuthorities());

        rateLimitService.resetLoginAttempts(clientIp, identifier);

        return LoginResponse.from(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        Long memberId = tokenProvider.validateRefreshTokenAndGetMemberId(refreshToken);

        String storedHash =
                refreshTokenRepository.findByMemberId(memberId).orElseThrow(RefreshTokenNotFoundException::new);

        if (!TokenHashUtil.matches(refreshToken, storedHash)) {
            refreshTokenRepository.deleteByMemberId(memberId);
            log.warn("Refresh token reuse detected! memberId: {}", memberId);
            throw new TokenReuseDetectedException();
        }

        Member member = memberFinder.getActiveById(memberId);

        TokenPair tokens = tokenPairCreateService.createTokens(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())));

        return new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
