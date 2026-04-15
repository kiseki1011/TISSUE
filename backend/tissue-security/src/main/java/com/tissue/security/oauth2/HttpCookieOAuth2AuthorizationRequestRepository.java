package com.tissue.security.oauth2;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("NullAway")
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    private static final ObjectMapper objectMapper = createObjectMapper();

    private final TissueSecurityProperties tissueSecurityProperties;

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(
                HttpCookieOAuth2AuthorizationRequestRepository.class.getClassLoader()));
        return mapper;
    }

    @Override
    public @Nullable OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        boolean secure = tissueSecurityProperties.getCookie().isSecure();

        CookieUtil.addCookie(
                response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS,
                secure);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin) && isAuthorizedRedirectUri(redirectUriAfterLogin)) {
            CookieUtil.addCookie(
                    response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS, secure);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = tissueSecurityProperties.getCookie().isSecure();
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, secure);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME, secure);
    }

    public boolean isAuthorizedRedirectUri(String uri) {
        List<String> allowedOrigins = tissueSecurityProperties.getOauth2().getAllowedRedirectOrigins();
        if (allowedOrigins.isEmpty()) {
            return false;
        }
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

    private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            String json = objectMapper.writeValueAsString(authorizationRequest);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize OAuth2AuthorizationRequest", e);
        }
    }

    private static @Nullable OAuth2AuthorizationRequest deserialize(String cookieValue) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cookieValue);
            return objectMapper.readValue(decoded, OAuth2AuthorizationRequest.class);
        } catch (IOException e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequest", e);
            return null;
        }
    }
}
