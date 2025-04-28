package com.tissue.api.security.authorization.interceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class SelfOrRoleRequiredInterceptorTest {

	@Mock
	private SessionManager sessionManager;
	@Mock
	private WorkspaceMemberReader workspaceMemberReader;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private HttpSession session;
	@Mock
	private HandlerMethod handlerMethod;

	@InjectMocks
	private SelfOrRoleRequiredInterceptor interceptor;

	static class TestWorkspaceMember extends WorkspaceMember {
		private final WorkspaceRole role;

		public TestWorkspaceMember(WorkspaceRole role) {
			this.role = role;
		}

		@Override
		public WorkspaceRole getRole() {
			return role;
		}
	}

	@Test
	@DisplayName("annotation이 없으면 true 반환")
	void preHandle_returnsTrue_ifNoAnnotation() {

		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(null);

		boolean result = interceptor.preHandle(request, response, handlerMethod);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("자기 자신이면 true 반환")
	void preHandle_returnsTrue_ifIsSelf() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		Long loginMemberId = 100L;
		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.of(loginMemberId));

		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("workspaceCode", "WORKSPACE1");
		pathVars.put("memberId", loginMemberId.toString());
		when(annotation.memberIdParam()).thenReturn("memberId");
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		boolean result = interceptor.preHandle(request, response, handlerMethod);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("자기 자신이 아니고, 권한이 충분하면 true 반환")
	void preHandle_returnsTrue_ifNotSelfAndHasRole() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		Long loginMemberId = 101L;
		Long targetMemberId = 202L;
		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.of(loginMemberId));

		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("workspaceCode", "WORKSPACE2");
		pathVars.put("memberId", targetMemberId.toString());
		when(annotation.memberIdParam()).thenReturn("memberId");
		when(annotation.role()).thenReturn(WorkspaceRole.MANAGER);
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		// 권한 충분한 워크스페이스 멤버 모킹
		TestWorkspaceMember member = new TestWorkspaceMember(WorkspaceRole.MANAGER);
		when(workspaceMemberReader.findWorkspaceMember(loginMemberId, "WORKSPACE2"))
			.thenReturn(member);

		boolean result = interceptor.preHandle(request, response, handlerMethod);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("권한 부족시 ForbiddenOperationException 발생")
	void preHandle_throws_ifNotSelfAndNoRole() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		Long loginMemberId = 101L;
		Long targetMemberId = 202L;
		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.of(loginMemberId));

		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("workspaceCode", "WORKSPACE2");
		pathVars.put("memberId", targetMemberId.toString());
		when(annotation.memberIdParam()).thenReturn("memberId");
		when(annotation.role()).thenReturn(WorkspaceRole.MANAGER);
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		// 권한 낮은 멤버 모킹
		TestWorkspaceMember member = new TestWorkspaceMember(WorkspaceRole.MEMBER);
		when(workspaceMemberReader.findWorkspaceMember(loginMemberId, "WORKSPACE2"))
			.thenReturn(member);

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(ForbiddenOperationException.class)
			.hasMessageContaining("Workspace role must be at least");
	}

	@Test
	@DisplayName("로그인 정보 없으면 AuthenticationFailedException")
	void preHandle_throws_ifNotLoggedIn() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	@DisplayName("workspaceCode 없으면 InvalidRequestException")
	void preHandle_throws_ifNoWorkspaceCode() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.of(1L));

		Map<String, String> pathVars = new HashMap<>();

		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	@DisplayName("memberId 없으면 InvalidRequestException")
	void preHandle_throws_ifNoMemberId() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);
		when(request.getSession(false)).thenReturn(session);

		when(sessionManager.getOptionalLoginMemberId(session)).thenReturn(Optional.of(1L));

		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("workspaceCode", "WORKSPACE1");
		when(annotation.memberIdParam()).thenReturn("memberId");
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InvalidRequestException.class);
	}
}