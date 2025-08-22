package com.tissue.unit.controller;

import com.tissue.support.helper.ControllerTestHelper;

class IssueControllerTest extends ControllerTestHelper {

	// @Test
	// @DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청에서 제목이 비어있으면 유효성 검사를 실패해서 BAD_REQUEST를 응답한다")
	// void createEpic_fails_ifTitleIsEmpty() throws Exception {
	// 	// given
	// 	String workspaceKey = "TESTCODE";
	//
	// 	CreateEpicRequest request = CreateEpicRequest.builder()
	// 		.common(CommonIssueCreateFields.builder()
	// 			.title("")
	// 			.content("Epic Content")
	// 			.summary("Epic Summary")
	// 			.priority(IssuePriority.HIGH)
	// 			.dueAt(LocalDateTime.now().plusDays(10))
	// 			.build())
	// 		.businessGoal("Business Goal")
	// 		.build();
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces/{code}/issues", workspaceKey)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces/{code}/issues - 에픽 이슈 생성에 성공하면 에픽 생성 응답을 응답 데이터로 받는다")
	// void createEpic_ValidRequest_ReturnsCreatedResponse() throws Exception {
	// 	// given
	// 	String workspaceKey = "TESTCODE";
	// 	String issueKey = "ISSUE-1";
	//
	// 	CreateEpicRequest request = CreateEpicRequest.builder()
	// 		.common(CommonIssueCreateFields.builder()
	// 			.title("Epic Title")
	// 			.content("Epic Content")
	// 			.summary("Epic Summary")
	// 			.priority(IssuePriority.HIGH)
	// 			.dueAt(LocalDateTime.now().plusDays(10))
	// 			.build())
	// 		.businessGoal("Business Goal")
	// 		.build();
	//
	// 	IssueResponse response = new IssueResponse(workspaceKey, issueKey);
	//
	// 	when(issueService.createIssue(anyString(), anyLong(), any(CreateIssueRequest.class)))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces/{code}/issues", workspaceKey)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isCreated())
	// 		.andExpect(jsonPath("$.data.workspaceKey").value(workspaceKey))
	// 		.andExpect(jsonPath("$.data.issueKey").value(issueKey))
	// 		.andExpect(jsonPath("$.message").value("Issue created."));
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 이슈 정보 업데이트에 성공하면 OK를 응답한다")
	// void updateIssue_Success() throws Exception {
	// 	// given
	// 	String workspaceKey = "TESTCODE";
	// 	String issueKey = "TEST-123";
	// 	LocalDateTime now = LocalDateTime.now();
	// 	LocalDateTime dueAt = LocalDateTime.now();
	//
	// 	UpdateStoryRequest request = UpdateStoryRequest.builder()
	// 		.common(CommonIssueUpdateFields.builder()
	// 			.title("Updated Title")
	// 			.content("Updated Content")
	// 			.summary("Updated Summary")
	// 			.priority(IssuePriority.HIGH)
	// 			.dueAt(dueAt)
	// 			.build())
	// 		.userStory("Updated User Story")
	// 		.acceptanceCriteria("Updated Acceptance Criteria")
	// 		.build();
	//
	// 	IssueResponse response = new IssueResponse(workspaceKey, issueKey);
	//
	// 	when(issueService.updateIssue(eq(workspaceKey), eq(issueKey), anyLong(), eq(request)))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}", workspaceKey, issueKey)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Issue details updated."))
	// 		.andExpect(jsonPath("$.data.issueKey").value(issueKey))
	// 		.andExpect(jsonPath("$.data.workspaceKey").value(workspaceKey))
	// 		.andDo(print());
	//
	// 	verify(issueService).updateIssue(eq(workspaceKey), eq(issueKey), anyLong(), eq(request));
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 요청 이슈 타입과 업데이트를 위해 조회한 이슈 타입이 불일치하면 요청이 실패한다")
	// void updateIssue_InvalidType_ThrowsException() throws Exception {
	// 	// given
	// 	UpdateStoryRequest request = UpdateStoryRequest.builder()
	// 		.common(CommonIssueUpdateFields.builder()
	// 			.title("Updated Title")
	// 			.content("Updated Content")
	// 			.summary("Updated Summary")
	// 			.priority(IssuePriority.HIGH)
	// 			.dueAt(LocalDateTime.now())
	// 			.build())
	// 		.userStory("Updated User Story")
	// 		.acceptanceCriteria("Updated Acceptance Criteria")
	// 		.build();
	//
	// 	when(issueService.updateIssue(any(), any(), any(), any()))
	// 		.thenThrow(new InvalidOperationException("Issue type mismatch"));
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}", "TESTCODE", "TEST-123")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andDo(print());
	// }

	// @Test
	// @DisplayName("PATCH /workspaces/{code}/issues/{issueKey}/parent - 이슈의 부모 이슈 등록에 성공하면 OK를 응답한다")
	// void assignParentIssue() throws Exception {
	// 	// given
	// 	String workspaceKey = "WORKSPACE";
	// 	String issueKey = "ISSUE-1";
	// 	String parentIssueKey = "ISSUE-999";
	// 	AssignParentIssueRequest request = new AssignParentIssueRequest(parentIssueKey);
	//
	// 	IssueResponse response = new IssueResponse(workspaceKey, issueKey);
	//
	// 	when(issueService.assignParentIssue(eq(workspaceKey), eq(issueKey), anyLong(), eq(request)))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/parent", workspaceKey, issueKey)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Parent issue assigned."))
	// 		.andExpect(jsonPath("$.data.workspaceKey").value(response.workspaceKey()))
	// 		.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("DELETE /workspaces/{code}/issues/{issueKey}/parent - 이슈의 부모 이슈 해제에 성공하면 OK를 응답한다")
	// void removeParentIssue_fromStory() throws Exception {
	// 	// given
	// 	String workspaceKey = "WORKSPACE";
	// 	String issueKey = "ISSUE-1";
	//
	// 	IssueResponse response = new IssueResponse(workspaceKey, issueKey);
	//
	// 	when(issueService.removeParentIssue(eq(workspaceKey), eq(issueKey), anyLong()))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(delete("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/parent", workspaceKey, issueKey)
	// 			.contentType(MediaType.APPLICATION_JSON))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Parent issue relationship removed."))
	// 		.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
	// 		.andDo(print());
	// }
}
