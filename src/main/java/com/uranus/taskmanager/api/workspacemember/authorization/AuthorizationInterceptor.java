package com.uranus.taskmanager.api.workspacemember.authorization;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.auth.exception.UserNotLoggedInException;
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

	/**
	 * Todo: 상수 정리
	 */
	private static final String WORKSPACE_PREFIX = "/api/v1/workspaces/";
	private static final int WORKSPACE_PREFIX_LENGTH = WORKSPACE_PREFIX.length();

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * Todo: preHandle() 가독성 좋은 코드로 리팩토링
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			RoleRequired roleRequired = handlerMethod.getMethodAnnotation(RoleRequired.class);

			if (roleRequired != null) {
				// 로그인된 사용자 정보 가져오기 (ArgumentResolver 사용 가능)
				HttpSession session = request.getSession(false);
				if (session == null || session.getAttribute(SessionKey.LOGIN_MEMBER) == null) {
					throw new UserNotLoggedInException();
				}

				// 로그인된 멤버의 ID 가져오기
				String loginId = (String)session.getAttribute(SessionKey.LOGIN_MEMBER);
				log.info("loginId from session = {}", loginId);

				// 요청 URL에서 workspaceCode 추출
				String uri = request.getRequestURI();
				String workspaceCode = extractWorkspaceCodeFromUri(uri);
				log.info("extracted workspaceCode = {}", workspaceCode);

				// workspaceCode를 통해 워크스페이스 찾기
				Workspace workspace = workspaceRepository.findByCode(workspaceCode)
					.orElseThrow(WorkspaceNotFoundException::new);

				// 해당 워크스페이스에 대한 사용자의 역할 가져오기
				WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceId(loginId,
						workspace.getId())
					.orElseThrow(MemberNotInWorkspaceException::new);

				if (isAccessDenied(workspaceMember.getRole(), roleRequired.roles())) {
					throw new InsufficientWorkspaceRoleException();
				}
			}
		}
		return true; // 권한 검사 통과 시 요청 진행
	}

	/**
	 * Todo: 리팩터링 필요
	 * 권한 수준: ADMIN > USER > READER
	 * 권한에 정수 value를 부여해서 부등호 형식으로 비교하도록 수정해보자
	 */
	private boolean isAccessDenied(WorkspaceRole userRole, WorkspaceRole[] requiredRoles) {
		// 접근 권한이 없을 경우 true 반환
		for (WorkspaceRole requiredRole : requiredRoles) {
			if (userRole == WorkspaceRole.ADMIN) {
				return false; // ADMIN은 모든 권한 허용
			}
			if (userRole == WorkspaceRole.USER && (requiredRole == WorkspaceRole.USER
				|| requiredRole == WorkspaceRole.READER)) {
				return false; // USER는 USER와 READER API 접근 허용
			}
			if (userRole == WorkspaceRole.READER && requiredRole == WorkspaceRole.READER) {
				return false; // READER는 READER API만 접근 허용
			}
		}
		return true; // 권한이 일치하지 않으면 접근 불가
	}

	/**
	 * Todo: 더 좋은 방식이 있는지 찾아보기
	 * API 설계에 따라 로직이 변할 수 있을 것 같음
	 */
	private String extractWorkspaceCodeFromUri(String uri) {
		// prefix가 URI에 있는지 확인
		int startIndex = uri.indexOf(WORKSPACE_PREFIX);
		if (startIndex != -1) {
			// startIndex + WORKSPACE_PREFIX_LENGTH부터 시작하여 8자리를 잘라내기
			return uri.substring(startIndex + WORKSPACE_PREFIX_LENGTH, startIndex + WORKSPACE_PREFIX_LENGTH + 8);
		}

		return ""; // 유효하지 않은 경우 빈 문자열 반환
	}
}
