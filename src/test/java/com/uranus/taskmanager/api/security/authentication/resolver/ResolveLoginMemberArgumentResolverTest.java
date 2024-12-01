package com.uranus.taskmanager.api.security.authentication.resolver;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.session.SessionManager;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ResolveLoginMemberArgumentResolverTest {

	@Mock
	private NativeWebRequest webRequest;
	@Mock
	private HttpSession session;
	@Mock
	private SessionManager sessionManager;

	@InjectMocks
	private LoginMemberArgumentResolver resolver;

	public void dummyMethod(@ResolveLoginMember Long loginMemberId) {
		// LoginMember 타입을 사용하는 더미 메서드
	}

	public void dummyMethodWithoutLoginMember(@ResolveLoginMember String loginMemberId) {
		// String 타입을 사용하는 더미 메서드(LoginMember 타입을 사용하지 않는)
	}

	public void dummyMethodWithoutAnnotation(Long loginMemberId) {
		// @ResolveLoginMember 애노테이션을 사용하지 않는 더미 메서드
	}

	@Test
	@DisplayName("supportsParameter는 파라미터가 Long 타입과 @ResolveLoginMember 애노테이션이 있으면 true를 반환한다")
	void supportsParameter_WhenLoginMemberTypeAndAnnotation_ReturnTrue() throws Exception {
		// given - LoginMemberDto 파라미터를 가진 dummyMethod의 MethodParameter 생성
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethod", Long.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("supportsParameter는 파라미터가 Long 타입이 아니면 false를 반환한다")
	void supportsParameter_WhenNotLoginMemberType_ReturnFalse() throws Exception {
		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethodWithoutLoginMember", String.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("supportsParameter는 파라미터가 @ResolveLoginMember 애노테이션을 사용하지 않는 경우 false를 반환한다")
	void supportsParameter_WhenNoAnnotation_ReturnFalse() throws Exception {
		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethodWithoutAnnotation", Long.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("로그인된 멤버의 id를 반환한다")
	void resolveArgument_WhenLoggedIn_ReturnLoginMember() {
		// given
		Long loginMemberId = 1L;
		when(sessionManager.getSession(webRequest)).thenReturn(session);
		when(sessionManager.getLoginMemberId(session)).thenReturn(Optional.of(loginMemberId));

		// when
		Long result = (Long)resolver.resolveArgument(null, null, webRequest, null);

		// then
		assertThat(result).isNotNull()
			.isInstanceOf(Long.class);

		verify(sessionManager).getSession(webRequest);
		verify(sessionManager).getLoginMemberId(session);
	}

	@Test
	@DisplayName("세션이 없으면 예외가 발생한다")
	void resolveArgument_WhenNoSession_ThrowUserNotLoggedInException() {
		// given
		when(sessionManager.getSession(webRequest)).thenThrow(UserNotLoggedInException.class);

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(UserNotLoggedInException.class);

		verify(sessionManager).getSession(webRequest);
		verifyNoMoreInteractions(sessionManager);
	}

	@Test
	@DisplayName("세션의 속성값이 존재하지 않으면 예외가 발생한다")
	void resolveArgument_WhenMemberNotFound_ThrowMemberNotFoundException() {
		// given
		when(sessionManager.getSession(webRequest)).thenReturn(session);
		when(sessionManager.getLoginMemberId(session)).thenThrow(new UserNotLoggedInException());

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(UserNotLoggedInException.class);

		verify(sessionManager).getSession(webRequest);
		verify(sessionManager).getLoginMemberId(session);
	}

}
