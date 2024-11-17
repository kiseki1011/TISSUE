package com.uranus.taskmanager.mock;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.uranus.taskmanager.api.global.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;

public class MockLoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
	private final LoginMember loginMember;

	public MockLoginMemberArgumentResolver(LoginMember loginMember) {
		this.loginMember = loginMember;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ResolveLoginMember.class)
			&& parameter.getParameterType().equals(LoginMember.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		return loginMember;
	}
}
