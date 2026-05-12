package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.service.TokenPairCreateService;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.oauth2.CustomOAuth2User;
import com.tissue.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.tissue.security.oauth2.userinfo.OAuth2UserInfo;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private TokenPairCreateService tokenPairCreateService;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler sut;

    @Test
    @DisplayName("should redirect to signup page with register token when user is new")
    void onAuthenticationSuccess_NewUser_RedirectsToSignup() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Cookie redirectCookie = new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3000/callback");
        request.setCookies(redirectCookie);

        given(cookieAuthorizationRequestRepository.isAuthorizedRedirectUri("http://localhost:3000/callback"))
                .willReturn(true);

        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        given(userInfo.getEmail()).willReturn("new@gmail.com");
        given(userInfo.getName()).willReturn("New User");
        given(userInfo.getProvider()).willReturn("GOOGLE");
        given(userInfo.getProviderId()).willReturn("12345");

        CustomOAuth2User oauth2User = new CustomOAuth2User(null, userInfo);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oauth2User);

        given(tokenProvider.createRegisterToken(anyString(), anyString(), anyString()))
                .willReturn("fake-register-token");

        // when
        sut.onAuthenticationSuccess(request, response, authentication);

        // then
        String redirectedUrl = response.getRedirectedUrl();

        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        assertThat(redirectedUrl).contains("status=NEEDS_SIGNUP");
        assertThat(redirectedUrl).contains("registerToken=fake-register-token");
        assertThat(redirectedUrl).contains("email=new@gmail.com");
    }

    @Test
    @DisplayName("should redirect to login success with tokens when user exists")
    void onAuthenticationSuccess_ExistingUser_RedirectsToLoginSuccess() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Cookie redirectCookie = new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3000/callback");
        request.setCookies(redirectCookie);

        given(cookieAuthorizationRequestRepository.isAuthorizedRedirectUri("http://localhost:3000/callback"))
                .willReturn(true);

        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getEmail()).willReturn("existing@tissue.com");
        given(member.getUsername()).willReturn("honggildong");

        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);

        CustomOAuth2User oauth2User = new CustomOAuth2User(member, userInfo);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oauth2User);

        given(tokenPairCreateService.createTokens(anyLong(), anyString(), anyString(), any()))
                .willReturn(new TokenPair("access-token", "refresh-token"));

        // when
        sut.onAuthenticationSuccess(request, response, authentication);

        // then
        String redirectedUrl = response.getRedirectedUrl();

        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        assertThat(redirectedUrl).contains("status=LOGIN_SUCCESS");
        assertThat(redirectedUrl).contains("accessToken=access-token");
        assertThat(redirectedUrl).contains("refreshToken=refresh-token");

        then(tokenPairCreateService).should().createTokens(eq(1L), eq("existing@tissue.com"), eq("honggildong"), any());
    }

    @Test
    @DisplayName("should redirect with error when new user's email is not provided by OAuth provider")
    void onAuthenticationSuccess_NewUser_EmailNotProvided() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Cookie redirectCookie = new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3000/callback");
        request.setCookies(redirectCookie);

        given(cookieAuthorizationRequestRepository.isAuthorizedRedirectUri("http://localhost:3000/callback"))
                .willReturn(true);

        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        given(userInfo.getEmail()).willReturn(null);
        given(userInfo.getProvider()).willReturn("GITHUB");

        CustomOAuth2User oauth2User = new CustomOAuth2User(null, userInfo);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oauth2User);

        // when
        sut.onAuthenticationSuccess(request, response, authentication);

        // then
        String redirectedUrl = response.getRedirectedUrl();

        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        assertThat(redirectedUrl).contains("error=email_not_provided");
        assertThat(redirectedUrl).doesNotContain("status=NEEDS_SIGNUP");

        then(tokenProvider).shouldHaveNoInteractions();
    }
}
