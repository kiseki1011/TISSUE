package com.tissue.security.oauth2;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.oauth2.userinfo.OAuth2UserInfo;
import com.tissue.security.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
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
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String targetUrl = CookieUtil.getCookie(
                        request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(cookieAuthorizationRequestRepository::isAuthorizedRedirectUri)
                .orElse(getDefaultTargetUrl());

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
            String email = userInfo.getEmail();

            if (email == null) {
                log.warn("OAuth2 login failed: email not provided by {}", userInfo.getProvider());
                return UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("error", "email_not_provided")
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
}
