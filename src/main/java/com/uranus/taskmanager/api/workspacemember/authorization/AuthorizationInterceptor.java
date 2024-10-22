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

		// 로그인된 사용자 정보 가져오기
		String loginId = getLoginIdFromSession(request.getSession(false))
			.orElseThrow(UserNotLoggedInException::new);
		log.info("loginId from session = {}", loginId);

		// 요청 URL에서 workspaceCode 추출
		String workspaceCode = extractWorkspaceCodeFromUri(request.getRequestURI());
		log.info("extracted workspaceCode = {}", workspaceCode);

		// workspaceCode를 통해 워크스페이스 찾기
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		// 해당 워크스페이스에 대한 사용자의 역할 가져오기
		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceId(loginId,
				workspace.getId())
			.orElseThrow(MemberNotInWorkspaceException::new);

		checkIsRoleSufficient(workspaceMember, roleRequired);

		return true;
	}

	private void checkIsRoleSufficient(WorkspaceMember workspaceMember, RoleRequired roleRequired) {
		if (isAccessDenied(workspaceMember.getRole(), roleRequired.roles())) {
			throw new InsufficientWorkspaceRoleException();
		}
	}

	private static boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	private Optional<RoleRequired> getRoleRequired(Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod)handler;
		return Optional.ofNullable(handlerMethod.getMethodAnnotation(RoleRequired.class));
	}

	private Optional<String> getLoginIdFromSession(HttpSession session) {
		return Optional.ofNullable(session)
			.map(s -> (String)s.getAttribute(SessionKey.LOGIN_MEMBER));
	}

	/**
	 * 권한 수준: ADMIN > USER > READER
	 * 권한에 정수 value를 부여해서 부등호 형식으로 비교
	 */
	private boolean isAccessDenied(WorkspaceRole userRole, WorkspaceRole[] requiredRoles) {
		int userRoleLevel = userRole.getLevel(); // 예시: ADMIN=3, USER=2, READER=1
		return Arrays.stream(requiredRoles)
			.map(WorkspaceRole::getLevel)
			.noneMatch(requiredRoleLevel -> userRoleLevel >= requiredRoleLevel); // userRole이 충분한 권한을 갖고 있으면 false 반환
	}

	/**
	 * Todo
	 * 	- 사실상 정책이 코드에 하드코딩 되어 있다
	 * 	- 만약 코드가 8자리가 아닌 9자리를 사용하기로 하면 코드를 다시 수정해야 하는 일이 발생한다
	 * 	- 동적으로 코드 부분을 읽어서 추출하도록 로직을 구현하는 것이 좋을 것 같다
	 * 	- 다음의 문제도 있다
	 * 	  - 코드가 7자리면 ArrayIndexOutOfBoundException
	 * 	  - 코드가 9자리면 앞 8자리만 자른다
	 * 	- 코드가 8자리가 아니면 예외가 발생하도록 로직을 추가
	 * URI에서 WORKSPACE_PREFIX_LENGTH을 시작 인덱스로 시작해서 8자리 문자열을 추출한다
	 * 만약 URI의 길이를 계산해서 코드가 8자리가 아니라면 예외 발생
	 *
	 * @param uri - 현재 HTTP 요청의 URI
	 * @return String - URI에서 추출된 8자리의 워크스페이스 코드
	 */
	public String extractWorkspaceCodeFromUri(String uri) {
		return Optional.of(uri.indexOf(WORKSPACE_PREFIX))
			.filter(startIndex -> startIndex != -1)
			.map(startIndex -> {
				int codeStartIndex = startIndex + WORKSPACE_PREFIX_LENGTH;
				int codeEndIndex = codeStartIndex + 8;

				// URI의 길이가 워크스페이스 코드의 끝까지 충분한지 확인
				if (uri.length() != codeEndIndex) {
					throw new IllegalArgumentException("Workspace code must be 8 characters long");
				}
				return uri.substring(codeStartIndex, codeEndIndex); // 워크스페이스 코드 추출
			})
			.orElseThrow(() -> new IllegalArgumentException("Invalid workspace code in URI")); // Todo: 예외 만들기
	}

	private String extractWorkspaceCodeFromUri2(String uri) {
		return Optional.of(uri.indexOf(WORKSPACE_PREFIX))
			.filter(startIndex -> startIndex != -1)
			.map(startIndex -> uri.substring(startIndex + WORKSPACE_PREFIX_LENGTH,
				startIndex + WORKSPACE_PREFIX_LENGTH + 8))
			.orElseThrow(() -> new IllegalArgumentException("Invalid workspace code in URI")); // Todo: 예외 만들기
	}
}
