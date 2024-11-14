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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.security.authentication.constant.SessionKey;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.WorkspaceJoinResponse;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class WorkspaceMemberControllerTest extends ControllerTestHelper {

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

	@Test
	@DisplayName("POST /workspaces/{code}/invite - 워크스페이스 초대를 성공하면 초대 응답 객체를 데이터로 받는다")
	void test6() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

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

		when(workspaceMemberService.inviteMember(eq(workspaceCode), any(InviteMemberRequest.class)))
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
	@DisplayName("POST /workspaces/{code}/invites - 다수 멤버의 초대를 요청하는 경우 - 모든 멤버 초대 성공하는 경우 200을 응답받는다")
	void test9() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

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

		when(workspaceMemberService.inviteMembers(eq(workspaceCode), any(InviteMembersRequest.class)))
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
	@DisplayName("POST /workspaces/{code}/invites - 다수 멤버의 초대를 요청하는 경우 - 일부 멤버 초대를 실패해도 200을 응답받는다")
	void test10() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

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

		when(workspaceMemberService.inviteMembers(eq(workspaceCode), any(InviteMembersRequest.class)))
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
	@DisplayName("POST /workspaces/{code} - 워크스페이스 참여 요청을 성공하는 경우 200을 응답 받는다")
	void test11() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";
		String loginId = "member1";
		String email = "member1@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		Member member = memberEntityFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);
		WorkspaceJoinRequest request = new WorkspaceJoinRequest();

		WorkspaceJoinResponse response = WorkspaceJoinResponse.from(workspace, workspaceMember, false);

		when(workspaceMemberService.joinWorkspace(eq(workspaceCode),
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
	@DisplayName("POST /workspaces/{code} - 워크스페이스 참여 요청 시 비밀번호가 불일치하는 경우 401을 응답 받는다")
	void test12() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";
		String invalidPassword = "invalid1234!";

		WorkspaceJoinRequest request = new WorkspaceJoinRequest(invalidPassword);

		when(workspaceMemberService.joinWorkspace(eq("TESTCODE"), any(WorkspaceJoinRequest.class), anyLong()))
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
	@DisplayName("DELETE /workspaces/{code}/kick - 워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	void test13() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";

		Member member = memberEntityFixture.createMember("member1", "member1@test.com");
		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest("member1");

		KickWorkspaceMemberResponse response = KickWorkspaceMemberResponse.from("member1", workspaceMember);

		when(workspaceMemberService.kickWorkspaceMember(workspaceCode, request)).thenReturn(response);

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
