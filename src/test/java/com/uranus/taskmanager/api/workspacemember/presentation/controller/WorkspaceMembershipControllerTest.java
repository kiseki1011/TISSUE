package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.security.session.SessionAttributes;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.NoValidMembersToInviteException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickOutMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickOutMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateWorkspaceMemberRoleResponse;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class WorkspaceMembershipControllerTest extends ControllerTestHelper {

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
	@DisplayName("DELETE /workspaces/{code}/members/kick - 워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	void test13() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";

		Member member = memberEntityFixture.createMember("member1", "member1@test.com");
		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture
			.createCollaboratorWorkspaceMember(member, workspace);

		KickOutMemberRequest request = new KickOutMemberRequest("member1");

		KickOutMemberResponse response = KickOutMemberResponse.from("member1", workspaceMember);

		when(workspaceMemberCommandService.kickOutMember(eq(workspaceCode), eq(request), anyLong()))
			.thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}/members/kick", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member was kicked from this workspace"))
			.andDo(print());

	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/role - 워크스페이스 멤버의 권한을 변경하는데 성공하면 200을 응답받는다")
	void test14() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";

		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest(
			"target",
			WorkspaceRole.MANAGER
		);

		WorkspaceMember targetWorkspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build()
		);

		UpdateWorkspaceMemberRoleResponse response = UpdateWorkspaceMemberRoleResponse.from(targetWorkspaceMember);

		when(workspaceMemberCommandService.updateWorkspaceMemberRole(
			eq(workspaceCode),
			any(UpdateWorkspaceMemberRoleRequest.class),
			anyLong())
		).thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/role", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/role - 권한을 변경하는데 성공하면 해당 워크스페이스 멤버의 상세 정보를 응답 데이터로 받는다")
	void test15() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";

		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest(
			"target",
			WorkspaceRole.MANAGER
		);

		WorkspaceMember targetWorkspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build()
		);

		UpdateWorkspaceMemberRoleResponse response = UpdateWorkspaceMemberRoleResponse.from(targetWorkspaceMember);

		when(workspaceMemberCommandService.updateWorkspaceMemberRole(eq(workspaceCode),
			any(UpdateWorkspaceMemberRoleRequest.class),
			anyLong())
		).thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/role", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
			.andExpect(jsonPath("$.data.workspaceMemberDetail.workspaceRole").value("MANAGER"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 멤버 초대 성공")
	void inviteMembers_Success() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		String workspaceCode = "TESTCODE";
		Set<String> memberIdentifiers = new HashSet<>(Arrays.asList(
			"john@example.com",
			"jane@example.com"
		));
		InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);

		List<InviteMembersResponse.InvitedMember> invitedMembers = Arrays.asList(
			new InviteMembersResponse.InvitedMember(1L, "john@example.com"),
			new InviteMembersResponse.InvitedMember(2L, "jane@example.com")
		);

		InviteMembersResponse response = InviteMembersResponse.of(workspaceCode, invitedMembers);

		// 로그인 멤버 및 권한 설정
		when(workspaceMemberInviteService.inviteMembers(workspaceCode, request)).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invites", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("Members invited"))
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.data.totalInvitedMembers").value(2))
			.andExpect(jsonPath("$.data.invitedMembers[0].id").value(1))
			.andExpect(jsonPath("$.data.invitedMembers[0].email").value("john@example.com"))
			.andExpect(jsonPath("$.data.invitedMembers[1].id").value(2))
			.andExpect(jsonPath("$.data.invitedMembers[1].email").value("jane@example.com"))
			.andDo(print());

		verify(workspaceMemberInviteService).inviteMembers(workspaceCode, request);
	}

	@Test
	@DisplayName("비어있는 멤버 목록으로 초대 요청 시 요청 검증 실패")
	void inviteMembers_Fail_EmptyMemberList() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		String workspaceCode = "TESTCODE";
		Set<String> memberIdentifiers = new HashSet<>();
		InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invites", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have validation errors"));

		verify(workspaceMemberInviteService, never()).inviteMembers(any(), any());
	}

	@Test
	@DisplayName("모든 멤버 식별자가 초대 대상에서 제외되면 예외가 발생한다")
	void inviteMembers_ifAllIdentifiersExcluded_throwsException() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		String workspaceCode = "TESTCODE";
		Set<String> memberIdentifiers = Set.of("excludedMember1", "excludedMember2", "excludedMember3");
		InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);

		when(workspaceMemberInviteService.inviteMembers(workspaceCode, request)).thenThrow(
			new NoValidMembersToInviteException());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invites", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("No avaliable members were found for invitation."));
	}
}
