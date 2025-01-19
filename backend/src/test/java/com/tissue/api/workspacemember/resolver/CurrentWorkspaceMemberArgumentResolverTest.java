package com.tissue.api.workspacemember.resolver;

import static com.tissue.api.security.authorization.interceptor.AuthorizationInterceptor.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tissue.api.common.exception.type.InternalServerException;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CurrentWorkspaceMemberArgumentResolverTest {

	@InjectMocks
	private CurrentWorkspaceMemberArgumentResolver resolver;

	@Mock
	private MethodParameter methodParameter;
	@Mock
	private ModelAndViewContainer mavContainer;
	@Mock
	private NativeWebRequest webRequest;
	@Mock
	private WebDataBinderFactory binderFactory;
	@Mock
	private HttpServletRequest httpRequest;

	// 테스트를 위한 더미 메서드들
	public void dummyMethod(@CurrentWorkspaceMember Long workspaceMemberId) {
	}

	public void dummyMethodWithWrongType(@CurrentWorkspaceMember String workspaceMemberId) {
	}

	public void dummyMethodWithoutAnnotation(Long workspaceMemberId) {
	}

	@Test
	@DisplayName("Long 타입이고 @CurrentWorkspaceMember 애노테이션이 있으면 true를 반환한다")
	void supportsParameter_returnsTrue_whenLongTypeWithAnnotation() throws Exception {
		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethod", Long.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Long 타입이 아니면 false를 반환한다")
	void supportsParameter_returnsFalse_whenNotLongType() throws Exception {
		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethodWithWrongType", String.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("@CurrentWorkspaceMember 애노테이션이 없으면 false를 반환한다")
	void supportsParameter_returnsFalse_whenNoAnnotation() throws Exception {
		// given
		MethodParameter parameter = new MethodParameter(
			getClass().getDeclaredMethod("dummyMethodWithoutAnnotation", Long.class), 0);

		// when
		boolean result = resolver.supportsParameter(parameter);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("workspaceMemberId를 정상적으로 반환한다")
	void resolveArgument_returnsWorkspaceMemberId_whenAttributeExists() {
		// given
		when(webRequest.getNativeRequest()).thenReturn(httpRequest);
		Long expectedId = 1L;
		when(httpRequest.getAttribute(CURRENT_WORKSPACE_MEMBER_ID)).thenReturn(expectedId);

		// when
		Object result = resolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

		// then
		assertThat(result).isEqualTo(expectedId);
	}

	@Test
	@DisplayName("workspaceMemberId가 없으면 예외가 발생한다")
	void resolveArgument_throwsException_whenAttributeNotFound() {
		// given
		when(webRequest.getNativeRequest()).thenReturn(httpRequest);
		when(httpRequest.getAttribute(CURRENT_WORKSPACE_MEMBER_ID)).thenReturn(null);

		// when & then
		assertThatThrownBy(() ->
			resolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory))
			.isInstanceOf(InternalServerException.class);
	}
}