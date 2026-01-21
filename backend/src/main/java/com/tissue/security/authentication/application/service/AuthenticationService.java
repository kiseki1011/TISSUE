package com.tissue.security.authentication.application.service;

import com.tissue.security.authentication.application.port.in.AuthenticationUseCase;
import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.application.port.out.TokenProvider;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.domain.exception.AuthenticationErrorCode;
import com.tissue.security.authentication.domain.exception.InvalidTokenException;
import com.tissue.security.authentication.domain.exception.RefreshTokenReusedException;
import com.tissue.security.authentication.infrastructure.context.MemberDetailsService;
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
    private final TokenProvider tokenProvider;
    private final MemberDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public LoginResponse login(String loginEmail, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

        String accessToken = tokenProvider.createAccessToken(userDetails.getMemberId(), userDetails.getEmail());
        String refreshToken = tokenProvider.createRefreshToken(userDetails.getMemberId(), userDetails.getEmail());

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
                .orElseThrow(
                        () -> new InvalidTokenException(AuthenticationErrorCode.INVALID_TOKEN.getDefaultMessage()));

        if (!storedToken.equals(refreshToken)) {
            refreshTokenRepository.deleteByEmail(loginEmail);
            log.warn("Refresh Token Reuse Detected! Email: {}", loginEmail);
            throw new RefreshTokenReusedException(AuthenticationErrorCode.REFRESH_TOKEN_REUSED.getDefaultMessage());
        }

        // TODO: 이게 굳이 필요한가? 그냥 토큰의 loginEmail을 사용하면 되는거 아닌가?
        MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(loginEmail);

        String newAccessToken = tokenProvider.createAccessToken(userDetails.getMemberId(), userDetails.getEmail());
        String newRefreshToken = tokenProvider.createRefreshToken(userDetails.getMemberId(), userDetails.getEmail());

        refreshTokenRepository.save(
                userDetails.getEmail(),
                newRefreshToken,
                Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public ElevatedTokenResponse elevatePermission(String loginEmail, String password, Long memberId) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginEmail, password));

        String elevatedToken = tokenProvider.createElevatedToken(memberId, loginEmail);

        return new ElevatedTokenResponse(elevatedToken);
    }

    @Override
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
