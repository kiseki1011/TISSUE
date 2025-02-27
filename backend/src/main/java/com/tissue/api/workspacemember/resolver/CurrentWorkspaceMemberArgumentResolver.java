package com.tissue.api.workspacemember.resolver;

import static com.tissue.api.security.authorization.interceptor.AuthorizationInterceptor.*;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tissue.api.common.exception.type.InternalServerException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentWorkspaceMemberArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return isLongType(parameter)
			&& hasResolveCurrentWorkspaceMemberAnnotation(parameter);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();

		Long workspaceMemberId = (Long)request.getAttribute(CURRENT_WORKSPACE_MEMBER_ID);

		if (workspaceMemberId == null) {
			log.error("CURRENT_WORKSPACE_MEMBER_ID attribute not found in request context.");
			throw new InternalServerException("Failed to retrieve workspace member id.");
		}

		return workspaceMemberId;
	}

	private boolean hasResolveCurrentWorkspaceMemberAnnotation(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentWorkspaceMember.class);
	}

	private boolean isLongType(MethodParameter parameter) {
		return parameter.getParameterType().equals(Long.class);
	}
}
