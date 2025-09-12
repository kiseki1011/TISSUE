package deprecated.com.tissue.unit.security.authorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequiredInterceptor;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import deprecated.com.tissue.support.fixture.entity.MemberEntityFixture;
import deprecated.com.tissue.support.fixture.entity.WorkspaceEntityFixture;
import deprecated.com.tissue.support.fixture.entity.WorkspaceMemberEntityFixture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RoleRequiredInterceptorTest {

	public static final String TEST_WORKSPACE_CODE = "TESTCODE";
	public static final String TEST_LOGIN_ID = "user123";
	public static final String TEST_EMAIL = "user123@test.com";

	WorkspaceEntityFixture workspaceEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	MemberEntityFixture memberEntityFixture;

	@Mock
	private WorkspaceMemberFinder workspaceMemberFinder;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private HandlerMethod handlerMethod;
	@InjectMocks
	private RoleRequiredInterceptor roleRequiredInterceptor;

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
		boolean result = roleRequiredInterceptor.preHandle(request, response, nonHandlerMethod);

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
		boolean result = roleRequiredInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("로그인 되지 않은 사용자일 경우 예외가 발생한다")
	void shouldThrow_UserNotLoggedInException_ifNotLoggedIn() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class))
			.thenReturn(mock(RoleRequired.class));

		// when & then
		assertThatThrownBy(() -> roleRequiredInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	@DisplayName("멤버가 워크스페이스에 속해있지 않을 경우 예외가 발생한다")
	void shouldThrow_MemberNotInWorkspaceException_ifMemberDidNotJoinWorkspace() {
		// given
		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(mock(RoleRequired.class));
		setAuthenticatedUser(1L);

		when(workspaceMemberFinder.findWorkspaceMember(1L, TEST_WORKSPACE_CODE))
			.thenThrow(WorkspaceMemberNotFoundException.class);

		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("workspaceKey", TEST_WORKSPACE_CODE);
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVariables);

		// when & then
		assertThatThrownBy(() -> roleRequiredInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(WorkspaceMemberNotFoundException.class);
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
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createMemberWorkspaceMember(
			member,
			workspace
		);

		setAuthenticatedUser(1L);
		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);

		when(workspaceMemberFinder.findWorkspaceMember(1L, TEST_WORKSPACE_CODE))
			.thenReturn(workspaceMember);

		when(roleRequired.role()).thenReturn(WorkspaceRole.MANAGER);

		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("workspaceKey", TEST_WORKSPACE_CODE);
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVariables);

		// when & then
		assertThatThrownBy(() -> roleRequiredInterceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(ForbiddenOperationException.class);
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
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createMemberWorkspaceMember(
			member,
			workspace
		);

		setAuthenticatedUser(1L);
		RoleRequired roleRequired = mock(RoleRequired.class);

		when(handlerMethod.getMethodAnnotation(RoleRequired.class)).thenReturn(roleRequired);

		when(workspaceMemberFinder.findWorkspaceMember(1L, TEST_WORKSPACE_CODE))
			.thenReturn(workspaceMember);

		when(roleRequired.role()).thenReturn(WorkspaceRole.MEMBER);

		Map<String, String> pathVariables = new HashMap<>();
		pathVariables.put("workspaceKey", TEST_WORKSPACE_CODE);
		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVariables);

		// when
		boolean result = roleRequiredInterceptor.preHandle(request, response, handlerMethod);

		// then
		assertThat(result).isTrue();
	}

	private void setAuthenticatedUser(Long memberId) {
		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(memberId);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
			List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
