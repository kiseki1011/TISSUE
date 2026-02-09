package com.tissue.global.security.oauth2;

import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.global.security.oauth2.userinfo.OAuth2UserInfo;
import com.tissue.global.security.util.CookieUtils;
import com.tissue.member.application.service.MemberValidator;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.UnauthorizedDomainException;
import com.tissue.system.Mode;
import com.tissue.system.SystemProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final SystemProperties systemProperties;
    private final MemberValidator memberValidator;

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

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        if (oauth2User.isRegistered()) {
            Member member = Objects.requireNonNull(oauth2User.getMember());
            String accessToken =
                    tokenProvider.createAccessToken(member.getId(), member.getEmail(), authentication.getAuthorities());
            String refreshToken = tokenProvider.createRefreshToken(
                    member.getId(), member.getEmail(), authentication.getAuthorities());

            refreshTokenRepository.save(
                    member.getEmail(),
                    refreshToken,
                    Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("status", "LOGIN_SUCCESS")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();
        } else {
            OAuth2UserInfo userInfo = oauth2User.getUserInfo();
            String email = Objects.requireNonNull(userInfo.getEmail(), "Email not found from provider");

            if (systemProperties.getMode() == Mode.PRIVATE) {
                try {
                    // TODO: Checking this here doesnt seem like a good idea. Potential circular dependency.
                    memberValidator.ensureAllowedDomain(email);
                } catch (UnauthorizedDomainException e) {
                    log.warn("OAuth2 login blocked: unauthorized domain={}", email);
                    return UriComponentsBuilder.fromUriString(targetUrl)
                            .queryParam("error", "Unauthorized Domain")
                            .build()
                            .toUriString();
                }
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

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
