package com.tissue.api.security.authorization.interceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import com.tissue.api.member.domain.Member;
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
import com.tissue.fixture.entity.MemberEntityFixture;
import com.tissue.fixture.entity.WorkspaceEntityFixture;
import com.tissue.fixture.entity.WorkspaceMemberEntityFixture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthorizationInterceptorTest {

	public static final String TEST_URI = "/api/v1/workspaces/TESTCODE";
	public static final String TEST_WORKSPACE_CODE = "TESTCODE";
	public static final String TEST_LOGIN_ID = "user123";
	public static final String TEST_EMAIL = "user123@test.com";
	WorkspaceEntityFixture workspaceEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	MemberEntityFixture memberEntityFixture;
	@Mock
	private WorkspaceRepository workspaceRepository;
	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private HttpSession session;
	@Mock
	private HandlerMethod handlerMethod;
	@Mock
	private SessionManager sessionManager;
	@Mock
	private WorkspaceCodeParser workspaceCodeParser;
	@InjectMocks
	private AuthorizationInterceptor authorizationInterceptor;

	@BeforeEach
	public void setUp() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
	}

	@Test
	@DisplayName("Handler가 HandlerMethod가 아닌 경우 preHandle()은 true를 반환한다")
	void preHandle_shouldReturn_true_ifHandlerIsNot_HandlerMethod() {
		// given
		Object nonHandlerMethod = new Object();

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, nonHandlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("@RoleRequired 애노테이션이 없는 경우 preHandle()은 true를 반환한다")
	void preHandle_shouldReturn_true_ifRoleRequiredAnnotation_notExist() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class))
			.thenReturn(null);

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("로그인 되지 않은 사용자일 경우 예외가 발생한다")
	void shouldThrow_UserNotLoggedInException_ifNotLoggedIn() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class))
			.thenReturn(mock(RoleRequired.class));

		when(request.getSession(false))
			.thenReturn(null);

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("워크스페이스 코드에 대한 워크스페이스가 존재하지 않는 경우 예외가 발생한다")
	void shouldThrow_WorkspaceNotFoundException_ifWorkspaceNotFound() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class))
			.thenReturn(mock(RoleRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class)))
			.thenReturn(Optional.of(1L));
		when(workspaceRepository.findByCode(TEST_WORKSPACE_CODE))
			.thenReturn(Optional.empty());

		when(request.getRequestURI())
			.thenReturn(TEST_URI);
		when(workspaceCodeParser.extractWorkspaceCode(TEST_URI))
			.thenReturn(TEST_WORKSPACE_CODE);

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Test
	@DisplayName("멤버가 워크스페이스에 속해있지 않을 경우 예외가 발생한다")
	void shouldThrow_MemberNotInWorkspaceException_ifMemberDidNotJoinWorkspace() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace(TEST_WORKSPACE_CODE);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(mock(RoleRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(workspaceRepository.findByCode(TEST_WORKSPACE_CODE)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.empty());

		when(request.getRequestURI()).thenReturn(TEST_URI);
		when(workspaceCodeParser.extractWorkspaceCode(TEST_URI))
			.thenReturn(TEST_WORKSPACE_CODE);

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Test
	@DisplayName("권한이 부족할 경우 예외가 발생한다")
	void shouldThrow_InsufficientWorkspaceRoleException_ifMemberHasLowerRoleThanNeeded() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace(TEST_WORKSPACE_CODE);
		Member member = memberEntityFixture.createMember(
			TEST_LOGIN_ID,
			TEST_EMAIL
		);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(
			member,
			workspace
		);

		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(workspaceRepository.findByCode(TEST_WORKSPACE_CODE)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));
		when(roleRequired.roles()).thenReturn(new WorkspaceRole[] {WorkspaceRole.MANAGER});

		when(request.getRequestURI()).thenReturn(TEST_URI);
		when(workspaceCodeParser.extractWorkspaceCode(TEST_URI))
			.thenReturn(TEST_WORKSPACE_CODE);

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InsufficientWorkspaceRoleException.class);
	}

	@Test
	@DisplayName("요청이 성공하는 경우 preHandle은 true를 반환한다")
	void preHandler_shouldReturn_true_ifRequestSuccess() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace(TEST_WORKSPACE_CODE);
		Member member = memberEntityFixture.createMember(
			TEST_LOGIN_ID,
			TEST_EMAIL
		);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(
			member,
			workspace
		);

		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(workspaceRepository.findByCode(TEST_WORKSPACE_CODE)).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));
		when(roleRequired.roles()).thenReturn(new WorkspaceRole[] {WorkspaceRole.MEMBER});

		when(request.getRequestURI()).thenReturn(TEST_URI);
		when(workspaceCodeParser.extractWorkspaceCode(TEST_URI))
			.thenReturn(TEST_WORKSPACE_CODE);

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

}
