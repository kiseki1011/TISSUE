package com.tissue.unit.controller;

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

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.tissue.api.workspacemember.presentation.dto.response.RemoveWorkspaceMemberResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateRoleResponse;
import com.tissue.support.fixture.entity.MemberEntityFixture;
import com.tissue.support.fixture.entity.WorkspaceEntityFixture;
import com.tissue.support.fixture.entity.WorkspaceMemberEntityFixture;
import com.tissue.support.helper.ControllerTestHelper;

class WorkspaceMembershipControllerTest extends ControllerTestHelper {

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
	}

	@Test
	@DisplayName("DELETE /workspaces/{code}/members/{memberId} - 워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	void test13() throws Exception {
		// given
		Member member = memberEntityFixture.createMember(
			"member1",
			"member1@test.com"
		);

		Workspace workspace = workspaceEntityFixture.createWorkspace("TESTCODE");

		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createMemberWorkspaceMember(
			member,
			workspace
		);

		RemoveWorkspaceMemberResponse response = RemoveWorkspaceMemberResponse.from(workspaceMember);

		when(workspaceMemberCommandService.removeWorkspaceMember(
			anyString(),
			anyLong(),
			anyLong())
		)
			.thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}/members/{memberId}", "TESTCODE", 2L)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member was removed from this workspace"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/display-name - 표시 이름(displayName)을 변경하는데 성공하면 200을 응답받는다")
	void testUpdateNickname_ifSuccess_return200() throws Exception {
		// given
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("tester")
				.email("test@test.com")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build()
		);

		workspaceMember.updateDisplayName("newNickname");

		UpdateNicknameResponse response = UpdateNicknameResponse.from(workspaceMember);

		when(workspaceMemberCommandService.updateDisplayName(
			anyString(),
			anyLong(),
			any(UpdateDisplayNameRequest.class))
		)
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/display-name", "TESTCODE")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new UpdateDisplayNameRequest("newNickname"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Display name updated."))
			.andExpect(jsonPath("$.data.updatedNickname").value("newNickname"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/{memberId}/role - 워크스페이스 멤버의 권한을 변경하는데 성공하면 200을 응답받는다")
	void test14() throws Exception {
		// given
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);

		WorkspaceMember target = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build()
		);

		UpdateRoleResponse response = UpdateRoleResponse.from(target);

		when(workspaceMemberCommandService.updateRole(
			anyString(),
			anyLong(),
			anyLong(),
			any(UpdateRoleRequest.class))
		)
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/{memberId}/role", "TESTCODE", 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/{memberId}/role - 권한을 변경하는데 성공하면 해당 워크스페이스 멤버의 상세 정보를 응답 데이터로 받는다")
	void test15() throws Exception {
		// given
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);

		WorkspaceMember target = workspaceMemberEntityFixture.createManagerWorkspaceMember(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.build(),
			Workspace.builder()
				.code("TESTCODE")
				.build()
		);

		UpdateRoleResponse response = UpdateRoleResponse.from(target);

		when(workspaceMemberCommandService.updateRole(
			anyString(),
			anyLong(),
			anyLong(),
			any(UpdateRoleRequest.class))
		)
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/members/{memberId}/role", "TESTCODE", 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
			.andExpect(jsonPath("$.data.updatedRole").value("MANAGER"))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/members/invite - 워크스페이스 멤버 초대 성공")
	void inviteMembers_Success() throws Exception {
		// given
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
		when(workspaceMemberInviteService.inviteMembers(
			workspaceCode,
			request)
		)
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("Members invited"))
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.data.invitedMembers[0].id").value(1))
			.andExpect(jsonPath("$.data.invitedMembers[0].email").value("john@example.com"))
			.andExpect(jsonPath("$.data.invitedMembers[1].id").value(2))
			.andExpect(jsonPath("$.data.invitedMembers[1].email").value("jane@example.com"))
			.andDo(print());

		verify(workspaceMemberInviteService).inviteMembers(workspaceCode, request);
	}

	@Test
	@DisplayName("POST /workspaces/{code}/members/invite - 비어있는 멤버 목록으로 초대 요청 시 요청 검증 실패")
	void inviteMembers_Fail_EmptyMemberList() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		Set<String> memberIdentifiers = new HashSet<>();
		InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have validation errors"));

		verify(workspaceMemberInviteService, never()).inviteMembers(any(), any());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/members/invite - 모든 멤버 식별자가 초대 대상에서 제외되면 예외가 발생한다")
	void inviteMembers_ifAllIdentifiersExcluded_throwsException() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		Set<String> memberIdentifiers = Set.of("excludedMember1", "excludedMember2", "excludedMember3");
		InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);

		when(workspaceMemberInviteService.inviteMembers(workspaceCode, request))
			.thenThrow(new InvalidOperationException("No members were available for invitation."));

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("No members were available for invitation."));
	}
}
