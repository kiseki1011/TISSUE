package com.uranus.taskmanager.api.security.authorization.interceptor;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.uranus.taskmanager.api.security.authorization.exception.InvalidWorkspaceCodeInUriException;
import com.uranus.taskmanager.api.security.session.SessionManager;
import com.uranus.taskmanager.api.security.session.SessionValidator;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {
	private static final String WORKSPACE_PREFIX = "/api/v1/workspaces/";
	private static final int WORKSPACE_PREFIX_LENGTH = 19;

	private final SessionManager sessionManager;
	private final SessionValidator sessionValidator;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

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

		String workspaceCode = extractWorkspaceCodeFromUri(request.getRequestURI());
		log.debug("Extracted workspace code from URI: {}", workspaceCode);

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember workspaceMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceId(memberId, workspace.getId())
			.orElseThrow(MemberNotInWorkspaceException::new);

		validateRole(workspaceMember, roleRequired);

		return true;
	}

	private static boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	private Optional<RoleRequired> getRoleRequired(Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod)handler;
		return Optional.ofNullable(handlerMethod.getMethodAnnotation(RoleRequired.class));
	}

	private void validateRole(WorkspaceMember workspaceMember, RoleRequired roleRequired) {
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
	private boolean isAccessDenied(WorkspaceRole userRole, WorkspaceRole[] requiredRoles) {
		int userRoleLevel = userRole.getLevel();
		return Arrays.stream(requiredRoles)
			.map(WorkspaceRole::getLevel)
			.noneMatch(requiredRoleLevel -> userRoleLevel >= requiredRoleLevel);
	}

	/**
	 * URI에서 WORKSPACE_PREFIX_LENGTH을 시작 인덱스로 시작해서 8자리 문자열을 추출한다
	 * 만약 URI의 길이를 계산해서 코드가 8자리가 아니라면 예외 발생
	 *
	 * @param uri - 현재 HTTP 요청의 URI
	 * @return String - URI에서 추출된 워크스페이스 코드
	 */
	public String extractWorkspaceCodeFromUri(String uri) {
		return Optional.of(uri.indexOf(WORKSPACE_PREFIX))
			.filter(startIndex -> startIndex != -1)
			.map(startIndex -> {
				int codeStartIndex = startIndex + WORKSPACE_PREFIX_LENGTH;
				int slashIndex = uri.indexOf("/", codeStartIndex);

				return (slashIndex == -1)
					? uri.substring(codeStartIndex) : uri.substring(codeStartIndex, slashIndex);
			})
			.filter(this::isNotEmpty)
			.orElseThrow(InvalidWorkspaceCodeInUriException::new);
	}

	private boolean isNotEmpty(String code) {
		return !code.isEmpty();
	}
}
