package com.tissue.unit.controller;

import com.tissue.support.helper.ControllerTestHelper;

class WorkspaceMembershipControllerTest extends ControllerTestHelper {

	// WorkspaceEntityFixture workspaceEntityFixture;
	// MemberEntityFixture memberEntityFixture;
	// WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	//
	// @BeforeEach
	// public void setup() {
	// 	workspaceEntityFixture = new WorkspaceEntityFixture();
	// 	memberEntityFixture = new MemberEntityFixture();
	// 	workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
	// }
	//
	// @Test
	// @DisplayName("DELETE /workspaces/{code}/members/{memberId} - 워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	// void test13() throws Exception {
	// 	// given
	// 	doNothing().when(workspaceMemberService)
	// 		.removeWorkspaceMember(anyString(), anyLong(), anyLong());
	//
	// 	// when & then
	// 	mockMvc.perform(delete("/api/v1/workspaces/{code}/members/{memberId}", "TESTCODE", 2L)
	// 			.contentType(MediaType.APPLICATION_JSON))
	// 		.andExpect(status().isNoContent())
	// 		.andExpect(jsonPath("$.message").value("Member was removed from this workspace"))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{code}/members/display-name - 표시 이름(displayName)을 변경하는데 성공하면 200을 응답받는다")
	// void testUpdateNickname_ifSuccess_return200() throws Exception {
	// 	// given
	// 	WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createManagerWorkspaceMember(
	// 		Member.builder()
	// 			.loginId("tester")
	// 			.email("test@test.com")
	// 			.build(),
	// 		Workspace.builder()
	// 			.code("TESTCODE")
	// 			.build()
	// 	);
	//
	// 	workspaceMember.updateDisplayName("newNickname");
	//
	// 	WorkspaceMemberResponse response = WorkspaceMemberResponse.from(workspaceMember);
	//
	// 	when(workspaceMemberService.updateDisplayName(
	// 		anyString(),
	// 		anyLong(),
	// 		any(UpdateDisplayNameRequest.class))
	// 	)
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{code}/members/display-name", "TESTCODE")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(new UpdateDisplayNameRequest("newNickname"))))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Display name updated."))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{code}/members/{memberId}/role - 워크스페이스 멤버의 권한을 변경하는데 성공하면 200을 응답받는다")
	// void test14() throws Exception {
	// 	// given
	// 	UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);
	//
	// 	WorkspaceMember target = workspaceMemberEntityFixture.createManagerWorkspaceMember(
	// 		Member.builder()
	// 			.loginId("member1")
	// 			.build(),
	// 		Workspace.builder()
	// 			.code("TESTCODE")
	// 			.build()
	// 	);
	//
	// 	WorkspaceMemberResponse response = WorkspaceMemberResponse.from(target);
	//
	// 	when(workspaceMemberService.updateRole(
	// 		anyString(),
	// 		anyLong(),
	// 		anyLong(),
	// 		any(UpdateRoleRequest.class))
	// 	)
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{code}/members/{memberId}/role", "TESTCODE", 2L)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{code}/members/{memberId}/role - 권한을 변경하는데 성공하면 해당 워크스페이스 멤버의 상세 정보를 응답 데이터로 받는다")
	// void test15() throws Exception {
	// 	// given
	// 	UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);
	//
	// 	WorkspaceMember target = workspaceMemberEntityFixture.createManagerWorkspaceMember(
	// 		Member.builder()
	// 			.loginId("member1")
	// 			.email("member1@test.com")
	// 			.build(),
	// 		Workspace.builder()
	// 			.code("TESTCODE")
	// 			.build()
	// 	);
	//
	// 	WorkspaceMemberResponse response = WorkspaceMemberResponse.from(target);
	//
	// 	when(workspaceMemberService.updateRole(
	// 		anyString(),
	// 		anyLong(),
	// 		anyLong(),
	// 		any(UpdateRoleRequest.class))
	// 	)
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{code}/members/{memberId}/role", "TESTCODE", 2L)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Member's role for this workspace was updated"))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces/{code}/members/invite - 워크스페이스 멤버 초대 성공")
	// void inviteMembers_Success() throws Exception {
	// 	// given
	// 	String workspaceCode = "TESTCODE";
	// 	InviteMembersRequest request = InviteMembersRequest.of(Set.of("dummy1", "dummy2"));
	//
	// 	// invited member IDs (직접 생성)
	// 	List<Long> invitedMemberIds = List.of(1L, 2L);
	// 	InviteMembersResponse response = new InviteMembersResponse(workspaceCode, invitedMemberIds);
	//
	// 	// mock behavior 설정
	// 	when(workspaceMemberInviteService.inviteMembers(eq(workspaceCode), eq(request)))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.code").value("200"))
	// 		.andExpect(jsonPath("$.message").value("Members invited"))
	// 		.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
	// 		.andExpect(jsonPath("$.data.invitedMemberIds").isArray())
	// 		.andExpect(jsonPath("$.data.invitedMemberIds.length()").value(2))
	// 		.andDo(print());
	//
	// 	verify(workspaceMemberInviteService).inviteMembers(workspaceCode, request);
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces/{code}/members/invite - 비어있는 멤버 목록으로 초대 요청 시 요청 검증 실패")
	// void inviteMembers_Fail_EmptyMemberList() throws Exception {
	// 	// given
	// 	String workspaceCode = "TESTCODE";
	// 	Set<String> memberIdentifiers = new HashSet<>();
	// 	InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andExpect(jsonPath("$.message").value("One or more fields have failed validation."));
	//
	// 	verify(workspaceMemberInviteService, never()).inviteMembers(any(), any());
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces/{code}/members/invite - 모든 멤버 식별자가 초대 대상에서 제외되면 예외가 발생한다")
	// void inviteMembers_ifAllIdentifiersExcluded_throwsException() throws Exception {
	// 	// given
	// 	String workspaceCode = "TESTCODE";
	// 	Set<String> memberIdentifiers = Set.of("excludedMember1", "excludedMember2", "excludedMember3");
	// 	InviteMembersRequest request = InviteMembersRequest.of(memberIdentifiers);
	//
	// 	when(workspaceMemberInviteService.inviteMembers(workspaceCode, request))
	// 		.thenThrow(new InvalidOperationException("No members were available for invitation."));
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces/{code}/members/invite", workspaceCode)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andExpect(jsonPath("$.message").value("No members were available for invitation."));
	// }
}
