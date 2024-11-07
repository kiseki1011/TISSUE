package com.uranus.taskmanager.api.workspacemember.authorization;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.authorization.exception.InsufficientWorkspaceRoleException;
import com.uranus.taskmanager.api.workspacemember.authorization.exception.InvalidWorkspaceCodeInUriException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

	private static final String WORKSPACE_PREFIX = "/api/v1/workspaces/";
	private static final int WORKSPACE_PREFIX_LENGTH = 19;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	// Todo: 로깅 정리
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

		Long memberId = getLoginIdFromSession(request.getSession(false))
			.orElseThrow(UserNotLoggedInException::new);
		log.info("memberId from session = {}", memberId);

		String workspaceCode = extractWorkspaceCodeFromUri(request.getRequestURI());
		log.info("extracted workspaceCode = {}", workspaceCode);

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceId(memberId,
				workspace.getId())
			.orElseThrow(MemberNotInWorkspaceException::new);

		checkIsRoleSufficient(workspaceMember, roleRequired);

		return true;
	}

	private static boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	private Optional<RoleRequired> getRoleRequired(Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod)handler;
		return Optional.ofNullable(handlerMethod.getMethodAnnotation(RoleRequired.class));
	}

	private Optional<Long> getLoginIdFromSession(HttpSession session) {
		return Optional.ofNullable(session)
			.map(s -> (Long)s.getAttribute(SessionKey.LOGIN_MEMBER_ID));
	}

	private void checkIsRoleSufficient(WorkspaceMember workspaceMember, RoleRequired roleRequired) {
		if (isAccessDenied(workspaceMember.getRole(), roleRequired.roles())) {
			throw new InsufficientWorkspaceRoleException();
		}
	}

	/**
	 * 권한 수준: ADMIN > USER > READER
	 * 권한에 정수 level을 부여해서 부등호 형식으로 비교한다
	 * ADMIN - 3
	 * USER - 2
	 * READER - 1
	 *
	 * @param userRole - 사용자의 권한
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
	 * Todo
	 * 	- 테스트를 위해 public으로 열어두었다
	 * 	- private으로 닫고 레플리케이션을 사용해서 테스트하는 것을 고려하였으나
	 * 	- 데이터 수정 보다는 유틸성 메서드에 가깝기 때문에 그냥 public으로 열기로 했다
	 * 	- 해당 메서드와 유사한 로직을 다른 곳에 사용하게 된다면 따로 유틸 클래스로 분리 요망
	 * <p>
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
