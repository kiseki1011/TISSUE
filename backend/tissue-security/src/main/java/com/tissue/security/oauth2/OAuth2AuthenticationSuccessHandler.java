package com.tissue.security.oauth2;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.service.MemberAccountValidator;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.UnauthorizedDomainException;
import com.tissue.security.oauth2.userinfo.OAuth2UserInfo;
import com.tissue.security.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
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
    private final TokenPairCreateService tokenPairCreateService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthRequestRepository;
    private final MemberAccountValidator memberAccountValidator;
    private final TissueSecurityProperties tissueSecurityProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtil.getCookie(
                        request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(this::isAuthorizedRedirectUri);

        // fallback to default if no redirect uri found in cookie or not authorized
        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        if (oauth2User.isRegistered()) {
            Member member = Objects.requireNonNull(oauth2User.getMember());

            TokenPair tokens = tokenPairCreateService.createTokens(
                    member.getId(), member.getEmail(), member.getUsername(), authentication.getAuthorities());

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "LOGIN_SUCCESS")
                    .queryParam("accessToken", tokens.accessToken())
                    .queryParam("refreshToken", tokens.refreshToken())
                    .build()
                    .toUriString();
        } else {
            OAuth2UserInfo userInfo = oauth2User.getUserInfo();
            String email = Objects.requireNonNull(userInfo.getEmail(), "Email not found from provider");

            try {
                memberAccountValidator.ensureDomainAllowed(email);
            } catch (UnauthorizedDomainException e) {
                log.warn("OAuth2 login blocked: unauthorized domain={}", email);
                return UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("error", "Unauthorized Domain")
                        .build()
                        .toUriString();
            }

            String registerToken =
                    tokenProvider.createRegisterToken(userInfo.getProvider(), userInfo.getProviderId(), email);

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "NEEDS_SIGNUP")
                    .queryParam("registerToken", registerToken)
                    .queryParam("email", email)
                    .queryParam("name", userInfo.getName())
                    .build()
                    .toUriString();
        }
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        List<String> allowedOrigins = tissueSecurityProperties.getOauth2().getAllowedRedirectOrigins();
        if (allowedOrigins.contains("*")) {
            return true;
        }

        try {
            URI redirectUri = URI.create(uri);
            String redirectScheme = redirectUri.getScheme();
            String redirectHost = redirectUri.getHost();
            int redirectPort = redirectUri.getPort();

            return allowedOrigins.stream().anyMatch(allowedOrigin -> {
                try {
                    URI allowedUri = URI.create(allowedOrigin);
                    return Objects.equals(redirectScheme, allowedUri.getScheme())
                            && Objects.equals(redirectHost, allowedUri.getHost())
                            && redirectPort == allowedUri.getPort();
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid redirect URI: {}", uri);
            return false;
        }
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
