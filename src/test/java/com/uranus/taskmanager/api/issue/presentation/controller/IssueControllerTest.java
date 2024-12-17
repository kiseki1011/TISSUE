package com.uranus.taskmanager.api.issue.presentation.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.uranus.taskmanager.api.issue.domain.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.IssueStatus;
import com.uranus.taskmanager.api.issue.domain.IssueType;
import com.uranus.taskmanager.api.issue.presentation.dto.request.CreateIssueRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.response.CreateIssueResponse;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class IssueControllerTest extends ControllerTestHelper {
	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청을 성공하면 CREATED를 응답한다")
	void createIssue_Success() throws Exception {
		// given
		String code = "ABCDEFGH";
		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test content",
			IssuePriority.HIGH,
			LocalDate.now().plusDays(7),
			null
		);

		CreateIssueResponse expectedResponse = new CreateIssueResponse(
			1L,
			IssueType.TASK,
			"Test Issue",
			"Test content",
			IssuePriority.HIGH,
			IssueStatus.TODO,
			LocalDateTime.now(),
			LocalDate.now().plusDays(7),
			null
		);

		when(issueCommandService.createIssue(code, request))
			.thenReturn(expectedResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", code)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.issueId").value(expectedResponse.issueId()))
			.andExpect(jsonPath("$.data.title").value(expectedResponse.title()))
			.andExpect(jsonPath("$.message").value("Issue created."))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청의 제목이 비어있으면 BAD_REQUEST를 응답한다")
	void createIssue_EmptyTitle_BadRequest() throws Exception {
		// given
		String code = "ABCDEFGH";
		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.TASK,
			"",  // 빈 제목
			"Test content",
			IssuePriority.HIGH,
			LocalDate.now().plusDays(7),
			null
		);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", code)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/issues - 이슈 생성 요청에서 마감 날짜가 비어있으면 응답 데이터의 마감일은 null이다")
	void createIssue_EmptyDueDate_responseDueDateIsNull() throws Exception {
		// given
		String code = "ABCDEFGH";
		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test content",
			IssuePriority.HIGH,
			null,
			null
		);

		CreateIssueResponse expectedResponse = new CreateIssueResponse(
			1L,
			IssueType.TASK,
			"Test Issue",
			"Test content",
			IssuePriority.HIGH,
			IssueStatus.TODO,
			LocalDateTime.now(),
			null,
			null
		);

		when(issueCommandService.createIssue(code, request))
			.thenReturn(expectedResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/issues", code)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.dueDate").isEmpty())
			.andDo(print());
	}
}