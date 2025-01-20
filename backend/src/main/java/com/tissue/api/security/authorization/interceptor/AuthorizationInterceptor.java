package com.tissue.api.security.authorization.interceptor;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.UnauthorizedException;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {
	public static final String CURRENT_WORKSPACE_MEMBER_ID = "currentWorkspaceMemberId";

	private final SessionManager sessionManager;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeParser workspaceCodeParser;

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
			.orElseThrow(() -> new UnauthorizedException("Login is required to access."));

		String workspaceCode = workspaceCodeParser.extractWorkspaceCode(request.getRequestURI());
		log.debug("Extracted workspace code from URI: {}", workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceCode));

		validateRole(workspaceMember, roleRequired);

		request.setAttribute(CURRENT_WORKSPACE_MEMBER_ID, workspaceMember.getId());

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
