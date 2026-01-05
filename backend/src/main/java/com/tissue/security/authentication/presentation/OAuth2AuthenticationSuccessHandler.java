package com.tissue.security.authentication.presentation;

import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.application.port.out.TokenProvider;
import com.tissue.security.authentication.infrastructure.oauth.CustomOAuth2User;
import com.tissue.security.authentication.infrastructure.persistence.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.security.authentication.util.CookieUtils;
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

    private final TokenProvider tokenProvider;
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

        // fallback to default if no redirect uri found in cookie
        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // already a member -> login
        if (oAuth2User.getMember() != null) {
            String accessToken = tokenProvider.createAccessToken(
                    oAuth2User.getMember().getId(), oAuth2User.getMember().getEmail());
            String refreshToken = tokenProvider.createRefreshToken(
                    oAuth2User.getMember().getId(), oAuth2User.getMember().getEmail());

            refreshTokenRepository.save(
                    oAuth2User.getMember().getEmail(),
                    refreshToken,
                    Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "LOGIN_SUCCESS") // TODO: use constant
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();
        }
        // new user -> needs signup
        else {
            String registerToken = tokenProvider.createRegisterToken(
                    oAuth2User.getProvider(), oAuth2User.getIdentifier(), oAuth2User.getEmail());

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "NEEDS_SIGNUP") // TODO: use constant
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
