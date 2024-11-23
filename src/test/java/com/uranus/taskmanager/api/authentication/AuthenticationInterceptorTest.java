package com.uranus.taskmanager.api.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.authentication.interceptor.AuthenticationInterceptor;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.session.SessionAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private HttpSession session;
	@Mock
	private HandlerMethod handlerMethod;

	@InjectMocks
	private AuthenticationInterceptor authenticationInterceptor;

	@Test
	@DisplayName("Handler가 HandlerMethod가 아닌 경우 true를 반환한다")
	void test1() {
		// given
		Object nonHandlerMethod = new Object(); // HandlerMethod가 아닌 객체

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, nonHandlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("@LoginRequired 애노테이션이 없는 경우 true를 반환한다")
	void test2() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(null);  // LoginRequired 애노테이션이 없는 경우

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("세션이 없는 경우 UserNotLoggedInException이 발생한다")
	void test3() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(mock(LoginRequired.class));
		when(request.getSession(false)).thenReturn(null); // 세션이 없으면

		// when & then
		assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("세션에 로그인된 사용자가 없을 경우 UserNotLoggedInException이 발생한다")
	void test4() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(mock(LoginRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(null);  // 로그인 멤버가 없음

		// when & then
		assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("세션에 로그인된 사용자가 있는 경우 true를 반환한다")
	void test5() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(mock(LoginRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(session.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).thenReturn(anyString());

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

}