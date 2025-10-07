package deprecated.com.tissue.unit.controller;

import deprecated.com.tissue.support.helper.ControllerTestHelper;

class WorkspaceMemberDetailControllerTest extends ControllerTestHelper {

	private static final String BASE_URL = "/api/v1/workspaces/{workspaceKey}/members";

	// @Test
	// @DisplayName("PATCH /workspaces/{workspaceKey}/members/{memberId}/positions/{positionId} - 내 Position 할당을 성공하면 OK 응답")
	// void assignPosition_Success() throws Exception {
	// 	// Given
	// 	String workspaceKey = "TESTCODE";
	// 	Long targetMemberId = 1L;
	// 	Long loginMemberId = 1L;
	// 	Long positionId = 1L;
	//
	// 	WorkspaceMemberResponse response = new WorkspaceMemberResponse(workspaceKey, targetMemberId);
	//
	// 	when(workspaceMemberService.assignPosition(
	// 		workspaceKey,
	// 		positionId,
	// 		targetMemberId,
	// 		loginMemberId
	// 	))
	// 		.thenReturn(response);
	//
	// 	// When & Then
	// 	mockMvc.perform(
	// 			patch(BASE_URL + "/{memberId}/positions/{positionId}", workspaceKey, targetMemberId, positionId))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Position assigned to workspace member."))
	// 		.andExpect(jsonPath("$.data.memberId").value(targetMemberId));
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{workspaceKey}/members/positions/{positionId} - 내 Position 해제를 성공하면 OK 응답, 응답 데이터 없음")
	// void removePosition_Success() throws Exception {
	// 	// Given
	// 	String workspaceKey = "TESTCODE";
	// 	Long targetMemberId = 1L;
	// 	Long loginMemberId = 2L;
	// 	Long positionId = 1L;
	//
	// 	doNothing().when(workspaceMemberService)
	// 		.removePosition(workspaceKey, positionId, targetMemberId, loginMemberId);
	//
	// 	// When & Then
	// 	mockMvc.perform(
	// 			delete(BASE_URL + "/{memberId}/positions/{positionId}", workspaceKey, targetMemberId, positionId))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Position removed from workspace member."))
	// 		.andExpect(jsonPath("$.data").doesNotExist());
	// }

}