package com.tissue.application.service;

import com.tissue.application.dto.response.ElevatedTokenResponse;
import com.tissue.application.dto.response.LoginResponse;
import com.tissue.application.dto.response.RefreshTokenResponse;
import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.application.port.usecase.AuthenticationUseCase;
import com.tissue.domain.TokenProvider;
import com.tissue.domain.exception.RefreshTokenNotFoundException;
import com.tissue.domain.exception.TokenReuseDetectedException;
import com.tissue.principal.MemberDetails;
import com.tissue.principal.MemberDetailsService;
import java.time.Duration;
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
@Transactional
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
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

            String accessToken = tokenProvider.createAccessToken(
                    userDetails.getMemberId(),
                    userDetails.getEmail(),
                    userDetails.getName(),
                    userDetails.getAuthorities());

            String refreshToken = tokenProvider.createRefreshToken(
                    userDetails.getMemberId(),
                    userDetails.getEmail(),
                    userDetails.getName(),
                    userDetails.getAuthorities());

            refreshTokenRepository.save(
                    userDetails.getEmail(),
                    refreshToken,
                    Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

            rateLimitService.resetLoginAttempts(clientIp, loginEmail);

            return LoginResponse.from(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            rateLimitService.recordLoginFailure(clientIp, loginEmail);
            throw e;
        }
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        tokenProvider.validateRefreshToken(refreshToken);

        String loginEmail = tokenProvider.getSubjectFromToken(refreshToken);

        String storedToken =
                refreshTokenRepository.findByEmail(loginEmail).orElseThrow(RefreshTokenNotFoundException::new);

        if (!Objects.equals(storedToken, refreshToken)) {
            refreshTokenRepository.deleteByEmail(loginEmail);
            log.warn("Refresh Token Reuse Detected! Email: {}", loginEmail);
            throw new TokenReuseDetectedException();
        }

        MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(loginEmail);

        String newAccessToken = tokenProvider.createAccessToken(
                userDetails.getMemberId(),
                userDetails.getEmail(),
                userDetails.getNickname(),
                userDetails.getAuthorities());
        String newRefreshToken = tokenProvider.createRefreshToken(
                userDetails.getMemberId(),
                userDetails.getEmail(),
                userDetails.getNickname(),
                userDetails.getAuthorities());

        refreshTokenRepository.save(
                userDetails.getEmail(),
                newRefreshToken,
                Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public ElevatedTokenResponse elevatePermission(String loginEmail, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(loginEmail);

        String elevatedToken = tokenProvider.createElevatedToken(
                userDetails.getMemberId(),
                userDetails.getEmail(),
                userDetails.getNickname(),
                authentication.getAuthorities());

        return new ElevatedTokenResponse(elevatedToken);
    }

    @Override
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
