package com.tissue.mock;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tissue.api.security.authentication.resolver.ResolveLoginMember;

public class MockLoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
	private final Long loginMemberId;

	public MockLoginMemberArgumentResolver(Long loginMemberId) {
		this.loginMemberId = loginMemberId;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(ResolveLoginMember.class)
			&& parameter.getParameterType().equals(Long.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		return loginMemberId;
	}
}
