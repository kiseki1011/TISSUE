package com.tissue.unit.security.authorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequired;
import com.tissue.api.security.authorization.interceptor.SelfOrRoleRequiredInterceptor;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SelfOrRoleRequiredInterceptorTest {

	@Mock
	private WorkspaceMemberReader workspaceMemberReader;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
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

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
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

		Long loginMemberId = 100L;

		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(loginMemberId);

		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(auth);

		Map<String, String> pathVars = Map.of(
			"workspaceCode", "WORKSPACE1",
			"memberId", loginMemberId.toString()
		);

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

		Long loginMemberId = 101L;
		Long targetMemberId = 202L;

		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(loginMemberId);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(auth);

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

		Long loginMemberId = 101L;
		Long targetMemberId = 202L;

		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(loginMemberId);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(auth);

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
	void preHandle_ThrowsException_IfNotLoggedIn() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);

		SecurityContextHolder.clearContext();

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	@DisplayName("workspaceCode 없으면 InvalidRequestException")
	void preHandle_throws_ifNoWorkspaceCode() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);

		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(123L);

		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// do not include workspaceCode for pathVar
		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("memberId", "456"); // add only memberId

		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	@DisplayName("memberId 없으면 InvalidRequestException")
	void preHandle_throws_ifNoMemberId() {

		SelfOrRoleRequired annotation = mock(SelfOrRoleRequired.class);
		when(handlerMethod.getMethodAnnotation(SelfOrRoleRequired.class)).thenReturn(annotation);

		MemberUserDetails userDetails = mock(MemberUserDetails.class);
		when(userDetails.getMemberId()).thenReturn(123L);

		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of(() -> "ROLE_USER"));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// no not include memberId for pathVar
		Map<String, String> pathVars = new HashMap<>();
		pathVars.put("workspaceCode", "TESTCODE"); // add only workspaceCode

		when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVars);

		assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
			.isInstanceOf(InvalidRequestException.class);
	}
}