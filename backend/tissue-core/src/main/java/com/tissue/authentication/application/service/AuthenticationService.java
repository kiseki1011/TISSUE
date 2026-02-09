package com.tissue.authentication.application.service;

import com.tissue.authentication.application.dto.response.ElevatedTokenResponse;
import com.tissue.authentication.application.dto.response.LoginResponse;
import com.tissue.authentication.application.dto.response.RefreshTokenResponse;
import com.tissue.authentication.application.port.in.AuthenticationUseCase;
import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.global.security.exception.InvalidTokenException;
import com.tissue.global.security.principal.MemberDetails;
import com.tissue.global.security.principal.MemberDetailsService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Override
    public LoginResponse login(String loginEmail, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

        String accessToken = tokenProvider.createAccessToken(
                userDetails.getMemberId(), userDetails.getEmail(), userDetails.getName(), userDetails.getAuthorities());
        String refreshToken = tokenProvider.createRefreshToken(
                userDetails.getMemberId(), userDetails.getEmail(), userDetails.getName(), userDetails.getAuthorities());

        refreshTokenRepository.save(
                userDetails.getEmail(),
                refreshToken,
                Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

        return LoginResponse.from(accessToken, refreshToken);
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        tokenProvider.validateRefreshToken(refreshToken);

        String loginEmail = tokenProvider.getSubjectFromToken(refreshToken);

        String storedToken = refreshTokenRepository
                .findByEmail(loginEmail)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or expired"));

        if (!storedToken.equals(refreshToken)) {
            refreshTokenRepository.deleteByEmail(loginEmail);
            log.warn("Refresh Token Reuse Detected! Email: {}", loginEmail);
            throw new InvalidTokenException("Refresh token reuse detected");
        }

        MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(loginEmail);

        String newAccessToken = tokenProvider.createAccessToken(
                userDetails.getMemberId(), userDetails.getEmail(), userDetails.getNickname(), userDetails.getAuthorities());
        String newRefreshToken = tokenProvider.createRefreshToken(
                userDetails.getMemberId(), userDetails.getEmail(), userDetails.getNickname(), userDetails.getAuthorities());

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
