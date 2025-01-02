package com.tissue.api.issue.presentation.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.exception.IssueTypeMismatchException;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateEpicRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateEpicResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateStoryResponse;
import com.tissue.helper.ControllerTestHelper;

class IssueControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청에서 제목이 비어있으면 유효성 검사를 실패해서 BAD_REQUEST를 응답한다")
	void createEpic_fails_ifTitleIsEmpty() throws Exception {
		// given
		String workspaceCode = "TESTCODE";

		CreateEpicRequest request = CreateEpicRequest.builder()
			.title("") // 검증 실패
			.content("Epic Content")
			.summary("Epic Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now().plusDays(10))
			.businessGoal("Business Goal")
			.targetReleaseDate(LocalDate.now().plusMonths(1))
			.hardDeadLine(LocalDate.now().plusMonths(2))
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

		CreateEpicRequest request = CreateEpicRequest.builder()
			.title("Epic Title")
			.content("Epic Content")
			.summary("Epic Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now().plusDays(10))
			.businessGoal("Business Goal")
			.targetReleaseDate(LocalDate.now().plusMonths(1))
			.hardDeadLine(LocalDate.now().plusMonths(2))
			.build();

		CreateEpicResponse response = CreateEpicResponse.builder()
			.issueId(1L)
			.workspaceCode(workspaceCode)
			.title(request.title())
			.content(request.content())
			.businessGoal(request.businessGoal())
			.build();

		when(issueCommandService.createIssue(anyString(), any(CreateIssueRequest.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.issueId").value(1L))
			.andExpect(jsonPath("$.data.workspaceCode").value("TESTCODE"))
			.andExpect(jsonPath("$.data.title").value(request.title()))
			.andExpect(jsonPath("$.message").value("EPIC issue created."));
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 이슈 정보 업데이트에 성공하면 OK를 응답한다")
	void updateIssue_Success() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String issueKey = "TEST-123";
		LocalDateTime now = LocalDateTime.now();
		LocalDate dueDate = LocalDate.now();

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("Updated Title")
			.content("Updated Content")
			.summary("Updated Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(dueDate)
			.difficulty(Difficulty.HARD)
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		UpdateStoryResponse response = UpdateStoryResponse.builder()
			.issueId(1L)
			.issueKey(issueKey)
			.workspaceCode(workspaceCode)
			.updaterId(100L)
			.updatedAt(now)
			.title("Updated Title")
			.content("Updated Content")
			.summary("Updated Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(dueDate)
			.difficulty(Difficulty.HARD)
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		when(issueCommandService.updateIssue(workspaceCode, issueKey, request))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}", workspaceCode, issueKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Issue details updated."))
			.andExpect(jsonPath("$.data.issueId").value(1L))
			.andExpect(jsonPath("$.data.issueKey").value(issueKey))
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.data.updaterId").value(100L))
			.andExpect(jsonPath("$.data.title").value("Updated Title"))
			.andExpect(jsonPath("$.data.content").value("Updated Content"))
			.andExpect(jsonPath("$.data.summary").value("Updated Summary"))
			.andExpect(jsonPath("$.data.priority").value("HIGH"))
			.andExpect(jsonPath("$.data.difficulty").value("HARD"))
			.andExpect(jsonPath("$.data.userStory").value("Updated User Story"))
			.andExpect(jsonPath("$.data.acceptanceCriteria").value("Updated Acceptance Criteria"))
			.andDo(print());

		verify(issueCommandService).updateIssue(workspaceCode, issueKey, request);
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/issues/{issueKey} - 요청 이슈 타입과 업데이트를 위해 조회한 이슈 타입이 불일치하면 요청이 실패한다")
	void updateIssue_InvalidType_ThrowsException() throws Exception {
		// given
		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("Updated Title")
			.content("Updated Content")
			.summary("Updated Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.HARD)
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		when(issueCommandService.updateIssue(any(), any(), any()))
			.thenThrow(new IssueTypeMismatchException());

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
		String code = "WORKSPACE";
		String issueKey = "ISSUE-1";
		String parentIssueKey = "ISSUE-999";
		AssignParentIssueRequest request = new AssignParentIssueRequest(parentIssueKey);

		AssignParentIssueResponse response = AssignParentIssueResponse.builder()
			.issueId(1L)
			.issueKey(issueKey)
			.parentIssueId(2L)
			.parentIssueKey(parentIssueKey)
			.assignedAt(LocalDateTime.now())
			.build();

		when(issueCommandService.assignParentIssue(code, issueKey, request))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/issues/{issueKey}/parent", code, issueKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Parent issue assigned."))
			.andExpect(jsonPath("$.data.issueId").value(response.issueId()))
			.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
			.andExpect(jsonPath("$.data.parentIssueId").value(response.parentIssueId()))
			.andExpect(jsonPath("$.data.parentIssueKey").value(response.parentIssueKey()))
			.andDo(print());
	}

	@Test
	@DisplayName("DELETE /workspaces/{code}/issues/{issueKey}/parent - 이슈의 부모 이슈 해제에 성공하면 OK를 응답한다")
	void removeParentIssue_fromStory() throws Exception {
		// given
		String code = "WORKSPACE";
		String issueKey = "ISSUE-1";

		RemoveParentIssueResponse response = RemoveParentIssueResponse.builder()
			.issueId(1L)
			.issueKey(issueKey)
			.removedAt(LocalDateTime.now())
			.build();

		when(issueCommandService.removeParentIssue(code, issueKey))
			.thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}/issues/{issueKey}/parent", code, issueKey)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Parent issue relationship removed."))
			.andExpect(jsonPath("$.data.issueId").value(response.issueId()))
			.andExpect(jsonPath("$.data.issueKey").value(response.issueKey()))
			.andDo(print());
	}
}
