package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.tissue.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.security.oauth2.OAuth2AuthenticationFailureHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler sut;

    @Test
    @DisplayName("should redirect to redirect_uri with error param when cookie exists")
    void onAuthenticationFailure_With_RedirectUri() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Cookie redirectCookie = new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3000/callback");
        request.setCookies(redirectCookie);

        AuthenticationException exception = new AuthenticationServiceException("Provider error");

        // when
        sut.onAuthenticationFailure(request, response, exception);

        // then
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).contains("http://localhost:3000/callback");
        assertThat(redirectedUrl).contains("error=oauth2_authentication_failed");

        then(cookieRepository).should().removeAuthorizationRequestCookies(request, response);
    }

    @Test
    @DisplayName("should redirect to default URL with error param when no redirect_uri cookie")
    void onAuthenticationFailure_Without_RedirectUri() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = new AuthenticationServiceException("Provider error");

        // when
        sut.onAuthenticationFailure(request, response, exception);

        // then
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).contains("/");
        assertThat(redirectedUrl).contains("error=oauth2_authentication_failed");

        then(cookieRepository).should().removeAuthorizationRequestCookies(request, response);
    }
}
