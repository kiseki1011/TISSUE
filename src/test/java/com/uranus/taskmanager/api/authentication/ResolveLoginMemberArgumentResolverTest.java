package com.uranus.taskmanager.api.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.security.authentication.resolver.LoginMemberArgumentResolver;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;
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

	public void dummyMethod(@ResolveLoginMember LoginMember loginMember) {
		// LoginMember 타입을 사용하는 더미 메서드
	}

	public void dummyMethodWithoutLoginMember(@ResolveLoginMember String loginMember) {
		// String 타입을 사용하는 더미 메서드(LoginMember 타입을 사용하지 않는)
	}

	public void dummyMethodWithoutAnnotation(LoginMember loginMember) {
		// @ResolveLoginMember 애노테이션을 사용하지 않는 더미 메서드
	}

	@Test
	@DisplayName("supportsParameter는 파라미터가 LoginMember 타입과 @ResolveLoginMember 애노테이션이 있으면 true를 반환한다")
	void supportsParameter_WhenLoginMemberTypeAndAnnotation_ReturnTrue() throws Exception {
		// given - LoginMemberDto 파라미터를 가진 dummyMethod의 MethodParameter 생성
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethod", LoginMember.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("supportsParameter는 파라미터가 LoginMember 타입이 아니면 false를 반환한다")
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
			getClass().getDeclaredMethod("dummyMethodWithoutAnnotation", LoginMember.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("로그인된 회원 정보를 LoginMember로 변환해서 반환한다")
	void resolveArgument_WhenLoggedIn_ReturnLoginMember() {
		// given
		String loginId = "user123";
		String email = "user123@test.com";
		Member member = Member.builder()
			.loginId(loginId)
			.email(email)
			.build();

		when(sessionManager.getSession(webRequest)).thenReturn(session);
		when(sessionManager.getLoginMember(session)).thenReturn(member);

		// when
		LoginMember result = (LoginMember)resolver.resolveArgument(null, null, webRequest, null);

		// then
		assertThat(result).isNotNull()
			.isInstanceOf(LoginMember.class)
			.extracting("loginId", "email")
			.containsExactly(loginId, email);

		verify(sessionManager).getSession(webRequest);
		verify(sessionManager).getLoginMember(session);
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
	@DisplayName("회원을 찾을 수 없으면 MemberNotFoundException을 던진다")
	void resolveArgument_WhenMemberNotFound_ThrowMemberNotFoundException() {
		// given
		when(sessionManager.getSession(webRequest)).thenReturn(session);
		when(sessionManager.getLoginMember(session)).thenThrow(MemberNotFoundException.class);

		// when & then
		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
			.isInstanceOf(MemberNotFoundException.class);

		verify(sessionManager).getSession(webRequest);
		verify(sessionManager).getLoginMember(session);
	}

}
