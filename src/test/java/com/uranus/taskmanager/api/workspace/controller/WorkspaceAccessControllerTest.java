package com.uranus.taskmanager.api.workspace.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.authentication.LoginMemberArgumentResolver;
import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.api.workspace.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceJoinResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.service.WorkspaceAccessService;
import com.uranus.taskmanager.api.workspacemember.authorization.AuthorizationInterceptor;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;

@WebMvcTest(WorkspaceAccessController.class)
class WorkspaceAccessControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@MockBean
	private WorkspaceAccessService workspaceAccessService;
	@MockBean
	private LoginMemberArgumentResolver loginMemberArgumentResolver;
	@MockBean
	private AuthorizationInterceptor authorizationInterceptor;

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	InvitationEntityFixture invitationEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
		invitationEntityFixture = new InvitationEntityFixture();
	}

	/**
	 * Todo
	 *  - @WebMvcTest를 @MockBean과 사용
	 *  - Session, LoginMemberArgumentResolver, AuthorizationInterceptor를 모킹해야 함
	 *  -> 이걸 컨트롤러 단위 테스트를 진행할때 마다 하는건 귀찮음!
	 *  -> @WebMvcTest의 동작 과정을 찾아보자!
	 *  -> 물론 @BeforeEach를 사용해서 처리할 수 있겠지만, 테스트 간 결합도가 생김
	 */
	@Test
	@DisplayName("워크스페이스 초대를 성공하면 초대 응답 객체를 데이터로 받는다")
	void test6() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		Member member = memberEntityFixture.createMember(loginId, email);

		Invitation invitation = invitationEntityFixture.createPendingInvitation(workspace, member);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String requestBody = objectMapper.writeValueAsString(inviteMemberRequest);

		InviteMemberResponse inviteMemberResponse = InviteMemberResponse.from(invitation);

		when(workspaceAccessService.inviteMember(eq(workspaceCode), any(InviteMemberRequest.class)))
			.thenReturn(inviteMemberResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invite", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(workspaceCode))
			.andDo(print());
	}

	@Test
	@DisplayName("다수 멤버의 초대를 요청하는 경우 - 모든 멤버 초대 성공하는 경우 200을 응답받는다")
	void test9() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";
		String member1 = "member1";
		String member2 = "member2";
		List<String> memberIdentifiers = List.of(member1, member2);
		InviteMembersRequest inviteMembersRequest = new InviteMembersRequest(memberIdentifiers);

		List<InvitedMember> successfulResponses = List.of(
			InvitedMember.builder().loginId(member1).email("member1@test.com").build(),
			InvitedMember.builder().loginId(member2).email("member2@test.com").build()
		);

		List<FailedInvitedMember> failedResponses = List.of();

		InviteMembersResponse inviteMembersResponse = new InviteMembersResponse(successfulResponses, failedResponses);

		when(workspaceAccessService.inviteMembers(eq(workspaceCode), any(InviteMembersRequest.class)))
			.thenReturn(inviteMembersResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invites", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(inviteMembersRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.invitedMembers[0].loginId").value(member1))
			.andExpect(jsonPath("$.data.invitedMembers[1].loginId").value(member2))
			.andExpect(jsonPath("$.data.failedInvitedMembers").isEmpty())
			.andDo(print());
	}

	@Test
	@DisplayName("다수 멤버의 초대를 요청하는 경우 - 일부 멤버 초대를 실패해도 200을 응답받는다")
	void test10() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";
		String member1 = "member1";
		String member3 = "member3";
		String invalidMember = "invalidMember";

		List<String> memberIdentifiers = List.of(member1, invalidMember, member3);
		InviteMembersRequest inviteMembersRequest = new InviteMembersRequest(memberIdentifiers);

		List<InvitedMember> successfulResponses = List.of(
			InvitedMember.builder().loginId(member1).email("member1@test.com").build(),
			InvitedMember.builder().loginId(member3).email("member3@test.com").build()
		);

		List<FailedInvitedMember> failedResponses = List.of(
			FailedInvitedMember.builder()
				.identifier(invalidMember)
				.error(new MemberNotFoundException().getMessage())
				.build()
		);

		InviteMembersResponse inviteMembersResponse = InviteMembersResponse.builder()
			.invitedMembers(successfulResponses)
			.failedInvitedMembers(failedResponses)
			.build();

		when(workspaceAccessService.inviteMembers(eq(workspaceCode), any(InviteMembersRequest.class)))
			.thenReturn(inviteMembersResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invites", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(inviteMembersRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.invitedMembers[0].loginId").value(member1))
			.andExpect(jsonPath("$.data.invitedMembers[1].loginId").value(member3))
			.andExpect(jsonPath("$.data.failedInvitedMembers[0].identifier").value(invalidMember))
			.andExpect(jsonPath("$.data.failedInvitedMembers[0].error").value("Member was not found"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 참여 요청을 성공하는 경우 200을 응답 받는다")
	void test11() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";
		String loginId = "member1";
		String email = "member1@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		Member member = memberEntityFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(member, workspace);
		WorkspaceJoinRequest request = new WorkspaceJoinRequest();

		WorkspaceJoinResponse response = WorkspaceJoinResponse.from(workspace, workspaceMember, false);

		when(workspaceAccessService.joinWorkspace(eq(workspaceCode),
			any(WorkspaceJoinRequest.class),
			anyLong())).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Joined Workspace"))
			.andExpect(jsonPath("$.data.alreadyMember").value(false))
			.andDo(print());

	}

	@Test
	@DisplayName("워크스페이스 참여 요청 시 비밀번호가 불일치하는 경우 401을 응답 받는다")
	void test12() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";
		String invalidPassword = "invalid1234!";

		WorkspaceJoinRequest request = new WorkspaceJoinRequest(invalidPassword);

		when(workspaceAccessService.joinWorkspace(eq("TESTCODE"), any(WorkspaceJoinRequest.class), anyLong()))
			.thenThrow(new InvalidWorkspacePasswordException());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("The given workspace password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	void test13() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// LoginMemberArgumentResolver 모킹
		when(loginMemberArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(LoginMember.builder()
				.id(1L)
				.loginId("member1")
				.email("member1@test.com")
				.build());

		// AuthorizationInterceptor 모킹
		when(authorizationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		// given
		String workspaceCode = "TESTCODE";

		Member member = memberEntityFixture.createMember("member1", "member1@test.com");
		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(member, workspace);

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest("member1");

		KickWorkspaceMemberResponse response = KickWorkspaceMemberResponse.from("member1", workspaceMember);

		when(workspaceAccessService.kickWorkspaceMember(workspaceCode, request)).thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}/kick", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member was kicked from this Workspace"))
			.andDo(print());

	}
}
