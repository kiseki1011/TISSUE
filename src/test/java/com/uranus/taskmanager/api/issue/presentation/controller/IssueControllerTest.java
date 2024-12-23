package com.uranus.taskmanager.api.issue.presentation.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.presentation.dto.request.create.CreateEpicRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.response.create.CreateEpicResponse;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class IssueControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청에서 제목이 비어있으면 유효성 검사를 실패해서 BAD_REQUEST를 응답한다")
	void createEpic_fails_ifTitleIsEmpty() throws Exception {
		// given
		String workspaceCode = "TESTCODE";

		CreateEpicRequest request = new CreateEpicRequest(
			"", // 제목 유효성 검사 실패
			"Epic Content",
			"Epic Summary",
			IssuePriority.HIGH,
			LocalDate.now().plusDays(10),
			null,
			"Business Goal",
			LocalDate.now().plusMonths(1),
			LocalDate.now().plusMonths(2)
		);

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

		CreateEpicRequest request = new CreateEpicRequest(
			"Epic Title",
			"Epic Content",
			"Epic Summary",
			IssuePriority.HIGH,
			LocalDate.now().plusDays(10),
			null,
			"Business Goal",
			LocalDate.now().plusMonths(1),
			LocalDate.now().plusMonths(2)
		);

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
}
