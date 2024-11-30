package com.uranus.taskmanager.api.security.authentication;

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
import com.uranus.taskmanager.api.security.session.SessionValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private HandlerMethod handlerMethod;
	@Mock
	private SessionValidator sessionValidator;

	@InjectMocks
	private AuthenticationInterceptor authenticationInterceptor;

	@Test
	@DisplayName("Handler가 HandlerMethod가 아닌 경우 true를 반환한다")
	void preHandle_WhenNotHandlerMethod_ReturnTrue() {
		// given
		Object nonHandlerMethod = new Object(); // HandlerMethod가 아닌 객체

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, nonHandlerMethod);

		// then
		assertThat(result).isTrue();
		verifyNoInteractions(sessionValidator);
	}

	@Test
	@DisplayName("@LoginRequired 애노테이션이 없는 경우 true를 반환한다")
	void ppreHandle_WhenNoLoginRequired_ReturnTrue() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(null);  // LoginRequired 애노테이션이 없는 경우

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
		verifyNoInteractions(sessionValidator);
	}

	@Test
	@DisplayName("@LoginRequired 애노테이션이 있고 검증에 성공하면 true를 반환한다")
	void preHandle_WhenLoginRequiredAndValid_ReturnTrue() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(mock(LoginRequired.class));
		doNothing().when(sessionValidator).validateLoginStatus(request);

		// when
		boolean result = authenticationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
		verify(sessionValidator).validateLoginStatus(request);
	}

	@Test
	@DisplayName("@LoginRequired 애노테이션이 있고 검증에 실패하면 예외가 발생한다")
	void preHandle_WhenLoginRequiredAndInvalid_ThrowException() {
		// given
		when(handlerMethod.getMethodAnnotation(LoginRequired.class)).thenReturn(mock(LoginRequired.class));
		doThrow(UserNotLoggedInException.class)
			.when(sessionValidator).validateLoginStatus(request);

		// when & then
		assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(UserNotLoggedInException.class);
		verify(sessionValidator).validateLoginStatus(request);
	}
}