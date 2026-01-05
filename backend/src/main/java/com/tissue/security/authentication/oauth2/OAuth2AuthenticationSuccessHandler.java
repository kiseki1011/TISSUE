package com.tissue.security.authentication.oauth2;

import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.jwt.JwtTokenService;
import com.tissue.security.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(
                        request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        // Fallback to default if no redirect uri found in cookie
        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // Scenario 1: Already a member -> Login
        if (oAuth2User.getMember() != null) {
            String accessToken = jwtTokenService.createAccessToken(
                    oAuth2User.getMember().getId(), oAuth2User.getMember().getEmail());
            String refreshToken = jwtTokenService.createRefreshToken(
                    oAuth2User.getMember().getId(), oAuth2User.getMember().getEmail());

            refreshTokenRepository.save(
                    oAuth2User.getMember().getEmail(),
                    refreshToken,
                    Duration.ofSeconds(jwtTokenService.getRefreshTokenValidityInSeconds()));

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "LOGIN_SUCCESS")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();
        }

        // Scenario 2: New user -> Needs Signup
        else {
            String registerToken = jwtTokenService.createRegisterToken(
                    oAuth2User.getProvider(), oAuth2User.getIdentifier(), oAuth2User.getEmail());

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "NEEDS_SIGNUP")
                    .queryParam("registerToken", registerToken)
                    .queryParam("email", oAuth2User.getEmail()) // pre-fill email for convenience
                    .build()
                    .toUriString();
        }
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
