package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.domain.TokenProvider;
import com.tissue.feature.member.application.service.MemberValidator;
import com.tissue.feature.member.domain.Member;
import com.tissue.oauth2.CustomOAuth2User;
import com.tissue.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.oauth2.OAuth2AuthenticationSuccessHandler;
import com.tissue.oauth2.userinfo.OAuth2UserInfo;
import com.tissue.support.system.Mode;
import com.tissue.support.system.SystemProperties;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Mock
    private SystemProperties systemProperties;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler sut;

    @Test
    @DisplayName("should redirect to signup page with register token when user is new")
    void onAuthenticationSuccess_NewUser_RedirectsToSignup() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // setup SystemProperties.Mode.PUBLIC
        given(systemProperties.getMode()).willReturn(Mode.PUBLIC);

        // simulate that the frontend sent a cookie named "redirect_uri" with the value
        // "http://localhost:3000/callback"
        // the handler reads this cookie to decide where to redirect the user after login
        Cookie redirectCookie = new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3000/callback");
        request.setCookies(redirectCookie);

        // mock a OAuth2UserInfo object representing a user from Google
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        given(userInfo.getEmail()).willReturn("new@gmail.com");
        given(userInfo.getName()).willReturn("New User");
        given(userInfo.getProvider()).willReturn("GOOGLE");
        given(userInfo.getProviderId()).willReturn("12345");

        // create a CustomOAuth2User with 'member' as null, which means the user is not registered in our DB
        CustomOAuth2User oauth2User = new CustomOAuth2User(null, userInfo);

        // mock the Authentication object that Spring Security passes to the handler
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oauth2User);

        given(tokenProvider.createRegisterToken(anyString(), anyString(), anyString()))
                .willReturn("fake-register-token");

        // when
        sut.onAuthenticationSuccess(request, response, authentication);

        // then
        // verify the redirect URL constructed by the handler.
        String redirectedUrl = response.getRedirectedUrl();

        // should redirect to the callback URL from the cookie
        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        // should have 'status=NEEDS_SIGNUP' because the user is new
        assertThat(redirectedUrl).contains("status=NEEDS_SIGNUP");
        // should include the register token we mocked
        assertThat(redirectedUrl).contains("registerToken=fake-register-token");
        // should include user info for convenience
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

        // mock a Member entity that exists in our DB
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getEmail()).willReturn("existing@gmail.com");
        given(member.getName()).willReturn("Hong Gildong");

        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);

        // mock CustomOAuth2User
        // pass the mocked 'member' object
        CustomOAuth2User oauth2User = new CustomOAuth2User(member, userInfo);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oauth2User);

        given(tokenProvider.createAccessToken(anyLong(), anyString(), anyString(), any()))
                .willReturn("access-token");
        given(tokenProvider.createRefreshToken(anyLong(), anyString(), anyString(), any()))
                .willReturn("refresh-token");
        given(tokenProvider.getRefreshTokenValidityInSeconds()).willReturn(3600L);

        // when
        sut.onAuthenticationSuccess(request, response, authentication);

        // then
        String redirectedUrl = response.getRedirectedUrl();

        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        // should have 'status=LOGIN_SUCCESS' because the user exists
        assertThat(redirectedUrl).contains("status=LOGIN_SUCCESS");
        // should include the tokens we mocked
        assertThat(redirectedUrl).contains("accessToken=access-token");
        assertThat(redirectedUrl).contains("refreshToken=refresh-token");

        // verify that the refresh token was saved to the repository
        verify(refreshTokenRepository).save(eq("existing@gmail.com"), eq("refresh-token"), any());
    }
}
