package com.tissue.api.security.authorization.interceptor;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tissue.api.security.authentication.exception.UserNotLoggedInException;
import com.tissue.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.MemberNotInWorkspaceException;

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
	private final WorkspaceRepository workspaceRepository;
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

		Long memberId = sessionManager.getLoginMemberId(request.getSession(false))
			.orElseThrow(UserNotLoggedInException::new);

		String workspaceCode = workspaceCodeParser.extractWorkspaceCode(request.getRequestURI());
		log.debug("Extracted workspace code from URI: {}", workspaceCode);

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember workspaceMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceId(memberId, workspace.getId())
			.orElseThrow(MemberNotInWorkspaceException::new);

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
		if (isAccessDenied(workspaceMember.getRole(), roleRequired.roles())) {
			throw new InsufficientWorkspaceRoleException();
		}
	}

	/**
	 * 권한에 integer level을 부여해서 부등호 형식으로 비교한다
	 * 권한 수준: OWNER(4) > MANAGER(3) > COLLABORATOR(2) > VIEWER(1)
	 *
	 * @param userRole      - 사용자의 권한
	 * @param requiredRoles - 권한 리스트
	 * @return boolean - 접근 거부 여부(예시: 권한이 부족한 true 반환)
	 */
	private boolean isAccessDenied(
		WorkspaceRole userRole,
		WorkspaceRole[] requiredRoles
	) {
		int userRoleLevel = userRole.getLevel();
		return Arrays.stream(requiredRoles)
			.map(WorkspaceRole::getLevel)
			.noneMatch(requiredRoleLevel -> userRoleLevel >= requiredRoleLevel);
	}
}
