package com.tissue.api.security.authorization.interceptor;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleRequiredInterceptor implements HandlerInterceptor {

	private final SessionManager sessionManager;
	private final WorkspaceMemberRepository workspaceMemberRepository;

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

		RoleRequired roleRequired = getRoleRequired(handler)
			.orElse(null);

		if (roleRequired == null) {
			return true;
		}

		Long memberId = sessionManager.getOptionalLoginMemberId(request.getSession(false))
			.orElseThrow(() -> new AuthenticationFailedException("Login is required to access."));

		Map<String, String> pathVariables =
			(Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		String workspaceCode = Optional.ofNullable(pathVariables.get("workspaceCode"))
			.orElseThrow(() -> new InvalidRequestException("workspaceCode path variable is required."));

		log.debug("Extracted workspace code from URI: {}", workspaceCode);

		// TODO: Reader 사용
		WorkspaceMember workspaceMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceCode));

		validateRole(workspaceMember, roleRequired);

		return true;
	}

	private Optional<RoleRequired> getRoleRequired(Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod)handler;
		return Optional.ofNullable(handlerMethod.getMethodAnnotation(RoleRequired.class));
	}

	private void validateRole(
		WorkspaceMember workspaceMember,
		RoleRequired roleRequired
	) {
		boolean isLowerThanRequiredRole = workspaceMember.getRole().isLowerThan(roleRequired.role());

		if (isLowerThanRequiredRole) {
			throw new ForbiddenOperationException(String.format("Workspace role must be at least %s. Current role: %s",
				roleRequired.role(), workspaceMember.getRole()));
		}
	}

}
