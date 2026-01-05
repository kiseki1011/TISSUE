package com.tissue.security.authentication.application.service;

import com.tissue.security.authentication.application.port.in.AuthenticationUseCase;
import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.domain.MemberUserDetails;
import com.tissue.security.authentication.domain.exception.AuthenticationErrorCode;
import com.tissue.security.authentication.domain.exception.JwtAuthenticationException;
import com.tissue.security.authentication.infrastructure.jwt.JwtTokenProvider;
import com.tissue.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.security.authentication.presentation.dto.response.RefreshTokenResponse;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public LoginResponse login(String loginEmail, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.createAccessToken(userDetails.getMemberId(), userDetails.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getMemberId(), userDetails.getEmail());

        refreshTokenRepository.save(
                userDetails.getEmail(),
                refreshToken,
                Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds()));

        return LoginResponse.from(accessToken, refreshToken);
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        jwtTokenProvider.validateRefreshToken(refreshToken);

        String loginEmail = jwtTokenProvider.getSubjectFromToken(refreshToken);

        // TODO: cant i use optional instead of "if (storedToken == null)" ?
        String storedToken = refreshTokenRepository.findByEmail(loginEmail).orElse(null);

        // if the stored token is null, it means it expired or user logged out
        if (storedToken == null) {
            throw new JwtAuthenticationException(AuthenticationErrorCode.INVALID_TOKEN.getDefaultMessage());
        }

        // if the stored token differs from incoming, it's a reuse attempt
        if (!storedToken.equals(refreshToken)) {
            refreshTokenRepository.deleteByEmail(loginEmail);
            log.warn("Refresh Token Reuse Detected! Email: {}", loginEmail);
            throw new JwtAuthenticationException(AuthenticationErrorCode.REFRESH_TOKEN_REUSED.getDefaultMessage());
        }

        MemberUserDetails userDetails = (MemberUserDetails) userDetailsService.loadUserByUsername(loginEmail);

        String newAccessToken = jwtTokenProvider.createAccessToken(userDetails.getMemberId(), userDetails.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userDetails.getMemberId(), userDetails.getEmail());

        refreshTokenRepository.save(
                userDetails.getEmail(),
                newRefreshToken,
                Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds()));

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public ElevatedTokenResponse elevatePermission(String loginEmail, String password, Long memberId) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        String elevatedToken = jwtTokenProvider.createElevatedToken(memberId, loginEmail);

        return new ElevatedTokenResponse(elevatedToken);
    }

    @Override
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
