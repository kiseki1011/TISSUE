package com.tissue.mock;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMemberArgumentResolver;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockCurrentWorkspaceMemberArgumentResolver extends CurrentWorkspaceMemberArgumentResolver {

	private final Long currentWorkspaceMemberId;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentWorkspaceMember.class)
			&& parameter.getParameterType().equals(Long.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		return currentWorkspaceMemberId;
	}
}
