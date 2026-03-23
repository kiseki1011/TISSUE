package com.tissue.security.application.service;

import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.response.ElevatedTokenResponse;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.port.usecase.AuthenticationUseCase;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.RefreshTokenNotFoundException;
import com.tissue.security.domain.exception.TokenReuseDetectedException;
import com.tissue.security.principal.MemberDetails;
import com.tissue.security.principal.MemberDetailsService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final TokenPairCreateService tokenPairCreateService;
    private final MemberDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitService rateLimitService;

    @Override
    public LoginResponse login(String loginEmail, String password, String clientIp) {
        rateLimitService.checkLoginRateLimit(clientIp, loginEmail);

        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

            MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

            TokenPair tokens = tokenPairCreateService.createTokens(
                    userDetails.getMemberId(),
                    userDetails.getEmail(),
                    userDetails.getHandle(),
                    userDetails.getAuthorities());

            rateLimitService.resetLoginAttempts(clientIp, loginEmail);

            return LoginResponse.from(tokens.accessToken(), tokens.refreshToken());

        } catch (BadCredentialsException e) {
            rateLimitService.recordLoginFailure(clientIp, loginEmail);
            throw e;
        }
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        String loginEmail = tokenProvider.validateRefreshTokenAndGetSubject(refreshToken);

        String storedToken =
                refreshTokenRepository.findByEmail(loginEmail).orElseThrow(RefreshTokenNotFoundException::new);

        if (!Objects.equals(storedToken, refreshToken)) {
            refreshTokenRepository.deleteByEmail(loginEmail);
            log.warn("Refresh token reuse detected! email: {}", loginEmail);
            throw new TokenReuseDetectedException();
        }

        MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(loginEmail);

        TokenPair tokens = tokenPairCreateService.createTokens(
                userDetails.getMemberId(),
                userDetails.getEmail(),
                userDetails.getHandle(),
                userDetails.getAuthorities());

        return new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public ElevatedTokenResponse elevatePermission(String loginEmail, String password, String clientIp) {
        rateLimitService.checkLoginRateLimit(clientIp, loginEmail);

        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

            MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

            String elevatedToken = tokenProvider.createElevatedToken(
                    userDetails.getMemberId(),
                    userDetails.getEmail(),
                    userDetails.getHandle(),
                    authentication.getAuthorities());

            rateLimitService.resetLoginAttempts(clientIp, loginEmail);

            return new ElevatedTokenResponse(elevatedToken);

        } catch (BadCredentialsException e) {
            rateLimitService.recordLoginFailure(clientIp, loginEmail);
            throw e;
        }
    }

    @Override
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
