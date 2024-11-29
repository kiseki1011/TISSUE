package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickWorkspaceMemberResponse;
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
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest("member1");

		KickWorkspaceMemberResponse response = KickWorkspaceMemberResponse.from("member1", workspaceMember);

		when(workspaceMemberCommandService.kickWorkspaceMember(workspaceCode, request)).thenReturn(response);

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

		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		WorkspaceMember targetWorkspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build());

		UpdateWorkspaceMemberRoleResponse response = UpdateWorkspaceMemberRoleResponse.from(targetWorkspaceMember);

		when(workspaceMemberCommandService.updateWorkspaceMemberRole(eq(workspaceCode),
			any(UpdateWorkspaceMemberRoleRequest.class),
			anyLong())).thenReturn(response);

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

		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		WorkspaceMember targetWorkspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build());

		UpdateWorkspaceMemberRoleResponse response = UpdateWorkspaceMemberRoleResponse.from(targetWorkspaceMember);

		when(workspaceMemberCommandService.updateWorkspaceMemberRole(eq(workspaceCode),
			any(UpdateWorkspaceMemberRoleRequest.class),
			anyLong())).thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/role", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
			.andExpect(jsonPath("$.data.workspaceMemberDetail.email").value("member1@test.com"))
			.andExpect(jsonPath("$.data.workspaceMemberDetail.workspaceRole").value("MANAGER"))
			.andDo(print());
	}
}
