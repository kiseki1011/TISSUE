package com.tissue.security.authentication.application.service;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.MemberUserDetailsService;
import com.tissue.security.authentication.application.port.in.AuthenticationUseCase;
import com.tissue.security.authentication.jwt.JwtTokenService;
import com.tissue.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.security.authentication.presentation.dto.response.RefreshTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticationUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final MemberUserDetailsService userDetailsService;

    @Override
    @Transactional
    public LoginResponse login(String loginEmail, String password) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginEmail, password));

        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();

        String accessToken =
                jwtTokenService.createAccessToken(
                        userDetails.getMemberId(), userDetails.getEmail());
        String refreshToken =
                jwtTokenService.createRefreshToken(
                        userDetails.getMemberId(), userDetails.getEmail());

        return LoginResponse.from(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        // validate refresh token
        jwtTokenService.validateRefreshToken(refreshToken);

        // extract subject (login email)
        String loginEmail = jwtTokenService.getSubjectFromToken(refreshToken);

        // load user to ensure they still exist and are valid
        MemberUserDetails userDetails =
                (MemberUserDetails) userDetailsService.loadUserByUsername(loginEmail);

        // create new access token
        String newAccessToken =
                jwtTokenService.createAccessToken(
                        userDetails.getMemberId(), userDetails.getEmail());

        // RTR (Refresh Token Rotation): Issue a new refresh token
        String newRefreshToken =
                jwtTokenService.createRefreshToken(
                        userDetails.getMemberId(), userDetails.getEmail());

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public ElevatedTokenResponse elevatePermission(
            String loginEmail, String password, Long memberId) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginEmail, password));

        String elevatedToken = jwtTokenService.createElevatedToken(memberId, loginEmail);

        return new ElevatedTokenResponse(elevatedToken);
    }
}
