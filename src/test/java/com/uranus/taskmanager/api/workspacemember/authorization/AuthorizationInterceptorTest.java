package com.uranus.taskmanager.api.workspacemember.authorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.security.authentication.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.security.authentication.session.SessionManager;
import com.uranus.taskmanager.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.uranus.taskmanager.api.security.authorization.exception.InvalidWorkspaceCodeInUriException;
import com.uranus.taskmanager.api.security.authorization.interceptor.AuthorizationInterceptor;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class AuthorizationInterceptorTest {

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

	WorkspaceEntityFixture workspaceEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	MemberEntityFixture memberEntityFixture;

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
	void test1() {
		// given
		Object nonHandlerMethod = new Object();

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, nonHandlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("@RoleRequired 애노테이션이 없는 경우 preHandle()은 true를 반환한다")
	void test2() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(null);

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("로그인 되지 않은 사용자일 경우 예외가 발생한다")
	void test3() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(mock(RoleRequired.class));
		when(request.getSession(false)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(UserNotLoggedInException.class);
	}

	@Test
	@DisplayName("워크스페이스 코드에 대한 워크스페이스가 존재하지 않는 경우 예외가 발생한다")
	void test4() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(mock(RoleRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(request.getRequestURI()).thenReturn("/api/v1/workspaces/TESTCODE");
		when(workspaceRepository.findByCode("TESTCODE")).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@ParameterizedTest
	@CsvSource({
		"/api/v1/workspaces/WORKSPC123, WORKSPC123",    // URI 끝에 코드가 있는 경우
		"/api/v1/workspaces/WORKSPC123/some/path, WORKSPC123",  // URI 중간에 코드가 있는 경우
		"/api/v1/workspaces/ANOTHERCODE123/, ANOTHERCODE123"   // URI 끝에 슬래시가 있는 경우
	})
	@DisplayName("워크스페이스 코드 추출 로직은 '/api/v1/workspaces/' 부터, 다음 '/'전 또는 URI 끝까지의 문자열을 추출한다")
	void test8(String uri, String expectedCode) {
		// when
		String extractedCode = authorizationInterceptor.extractWorkspaceCodeFromUri(uri);

		// then
		assertThat(extractedCode).isEqualTo(expectedCode);
	}

	@DisplayName("다음 URI '/api/v1/workspaces/'에서 코드 없이 바로 '/'가 붙어있는 경우 예외가 발생한다")
	void test9() {
		// given
		String uri = "/api/v1/workspaces//";

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.extractWorkspaceCodeFromUri(uri))
			.isInstanceOf(InvalidWorkspaceCodeInUriException.class);
	}

	@Test
	@DisplayName("멤버가 워크스페이스에 속해있지 않을 경우 예외가 발생한다")
	void test5() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace("TESTCODE");

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(mock(RoleRequired.class));
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(request.getRequestURI()).thenReturn("/api/v1/workspaces/TESTCODE");
		when(workspaceRepository.findByCode("TESTCODE")).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Test
	@DisplayName("권한이 부족할 경우 예외가 발생한다")
	void test6() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace("TESTCODE");
		Member member = memberEntityFixture.createMember("user123", "user123@test.com");
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);

		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(request.getRequestURI()).thenReturn("/api/v1/workspaces/TESTCODE");
		when(workspaceRepository.findByCode("TESTCODE")).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));
		when(roleRequired.roles()).thenReturn(new WorkspaceRole[] {WorkspaceRole.MANAGER});

		// when & then
		assertThatThrownBy(() -> authorizationInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InsufficientWorkspaceRoleException.class);
	}

	@Test
	@DisplayName("요청이 성공하는 경우 preHandle은 true를 반환한다")
	void test7() {
		// given
		Workspace workspace = workspaceEntityFixture.createWorkspace("TESTCODE");
		Member member = memberEntityFixture.createMember("user123", "user123@test.com");
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);

		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);
		when(request.getSession(false)).thenReturn(session);
		when(sessionManager.getLoginMemberId(any(HttpSession.class))).thenReturn(Optional.of(1L));
		when(request.getRequestURI()).thenReturn("/api/v1/workspaces/TESTCODE");
		when(workspaceRepository.findByCode("TESTCODE")).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));
		when(roleRequired.roles()).thenReturn(new WorkspaceRole[] {WorkspaceRole.COLLABORATOR});

		// when
		boolean result = authorizationInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

}
