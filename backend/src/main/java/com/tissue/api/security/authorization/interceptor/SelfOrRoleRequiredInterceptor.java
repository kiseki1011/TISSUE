package com.tissue.api.security.authorization.interceptor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelfOrRoleRequiredInterceptor implements HandlerInterceptor {

	private final SessionManager sessionManager;
	private final WorkspaceMemberReader workspaceMemberReader;

	private static boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	@Override
	public boolean preHandle(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler
	) {
		if (isNotHandlerMethod(handler)) {
			return true;
		}

		HandlerMethod handlerMethod = (HandlerMethod)handler;
		SelfOrRoleRequired annotation = handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class);

		if (annotation == null) {
			return true;
		}

		Long loginMemberId = sessionManager.getOptionalLoginMemberId(request.getSession(false))
			.orElseThrow(() -> new AuthenticationFailedException("Login is required."));

		// --- 경로 변수에서 workspaceCode, memberId 추출 ---
		Map<String, String> pathVariables =
			(Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		String workspaceCode = Optional.ofNullable(pathVariables.get("workspaceCode"))
			.orElseThrow(() -> new InvalidRequestException("{workspaceCode} path variable is required."));

		String targetMemberIdStr = Optional.ofNullable(pathVariables.get(annotation.memberIdParam()))
			.orElseThrow(
				() -> new InvalidRequestException("{" + annotation.memberIdParam() + "} path variable is required."));
		Long targetMemberId = Long.parseLong(targetMemberIdStr);

		// --- 자기 자신이면 통과 ---
		boolean isSelf = Objects.equals(loginMemberId, targetMemberId);

		if (isSelf) {
			return true;
		}

		// --- 최소 Role 이상인지 확인 ---
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(loginMemberId, workspaceCode);
		validateRole(workspaceMember, annotation);

		return true;
	}

	private void validateRole(
		WorkspaceMember workspaceMember,
		SelfOrRoleRequired annotation
	) {
		boolean isLowerThanRequiredRole = workspaceMember.getRole().isLowerThan(annotation.role());

		if (isLowerThanRequiredRole) {
			throw new ForbiddenOperationException(String.format(
				"Workspace role must be at least %s. Current role: %s",
				annotation.role(), workspaceMember.getRole()));
		}
	}
}
