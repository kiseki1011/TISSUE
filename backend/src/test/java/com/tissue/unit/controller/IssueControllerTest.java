package com.tissue.unit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.presentation.controller.dto.request.AddParentIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.request.create.CommonIssueCreateFields;
import com.tissue.api.issue.presentation.controller.dto.request.update.CommonIssueUpdateFields;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.support.helper.ControllerTestHelper;

class IssueControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청에서 제목이 비어있으면 유효성 검사를 실패해서 BAD_REQUEST를 응답한다")
	void createEpic_fails_ifTitleIsEmpty() throws Exception {
		// given
		String workspaceCode = "TESTCODE";

		CreateEpicRequest request = CreateEpicRequest.builder()
			.common(CommonIssueCreateFields.builder()
				.title("")
				.content("Epic Content")
				.summary("Epic Summary")
				.priority(IssuePriority.HIGH)
				.dueAt(LocalDateTime.now().plusDays(10))
				.build())
			.businessGoal("Business Goal")
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 에픽 이슈 생성에 성공하면 에픽 생성 응답을 응답 데이터로 받는다")
	void createEpic_ValidRequest_ReturnsCreatedResponse() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String issueKey = "ISSUE-1";

		CreateEpicRequest request = CreateEpicRequest.builder()
			.common(CommonIssueCreateFields.builder()
				.title("Epic Title")
				.content("Epic Content")
				.summary("Epic Summary")
				.priority(IssuePriority.HIGH)
				.dueAt(LocalDateTime.now().plusDays(10))
				.build())
			.businessGoal("Business Goal")
			.build();

		IssueResponse response = new IssueResponse(workspaceCode, issueKey);

		when(issueCommandService.createIssue(anyString(), anyLong(), any(CreateIssueRequest.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.data.issueKey").value(issueKey))
			.andExpect(jsonPath("$.message").value("Issue created."));
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 이슈 정보 업데이트에 성공하면 OK를 응답한다")
	void updateIssue_Success() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String issueKey = "TEST-123";
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime dueAt = LocalDateTime.now();

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.common(CommonIssueUpdateFields.builder()
				.title("Updated Title")
				.content("Updated Content")
				.summary("Updated Summary")
				.priority(IssuePriority.HIGH)
				.dueAt(dueAt)
				.build())
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		IssueResponse response = new IssueResponse(workspaceCode, issueKey);

		when(issueCommandService.updateIssue(eq(workspaceCode), eq(issueKey), anyLong(), eq(request)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}", workspaceCode, issueKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Issue details updated."))
			.andExpect(jsonPath("$.data.issueKey").value(issueKey))
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andDo(print());

		verify(issueCommandService).updateIssue(eq(workspaceCode), eq(issueKey), anyLong(), eq(request));
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 요청 이슈 타입과 업데이트를 위해 조회한 이슈 타입이 불일치하면 요청이 실패한다")
	void updateIssue_InvalidType_ThrowsException() throws Exception {
		// given
		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.common(CommonIssueUpdateFields.builder()
				.title("Updated Title")
				.content("Updated Content")
				.summary("Updated Summary")
				.priority(IssuePriority.HIGH)
				.dueAt(LocalDateTime.now())
				.build())
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		when(issueCommandService.updateIssue(any(), any(), any(), any()))
			.thenThrow(new InvalidOperationException("Issue type mismatch"));

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}", "TESTCODE", "TEST-123")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/issues/{issueKey}/parent - 이슈의 부모 이슈 등록에 성공하면 OK를 응답한다")
	void assignParentIssue() throws Exception {
		// given
		String workspaceCode = "WORKSPACE";
		String issueKey = "ISSUE-1";
		String parentIssueKey = "ISSUE-999";
		AddParentIssueRequest request = new AddParentIssueRequest(parentIssueKey);

		IssueResponse response = new IssueResponse(workspaceCode, issueKey);

		when(issueCommandService.assignParentIssue(eq(workspaceCode), eq(issueKey), anyLong(), eq(request)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/parent", workspaceCode, issueKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Parent issue assigned."))
			.andExpect(jsonPath("$.data.workspaceCode").value(response.workspaceCode()))
			.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
			.andDo(print());
	}

	@Test
	@DisplayName("DELETE /workspaces/{code}/issues/{issueKey}/parent - 이슈의 부모 이슈 해제에 성공하면 OK를 응답한다")
	void removeParentIssue_fromStory() throws Exception {
		// given
		String workspaceCode = "WORKSPACE";
		String issueKey = "ISSUE-1";

		IssueResponse response = new IssueResponse(workspaceCode, issueKey);

		when(issueCommandService.removeParentIssue(eq(workspaceCode), eq(issueKey), anyLong()))
			.thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{workspaceCode}/issues/{issueKey}/parent", workspaceCode, issueKey)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Parent issue relationship removed."))
			.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
			.andDo(print());
	}
}
