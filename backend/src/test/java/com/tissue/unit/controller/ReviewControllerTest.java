package com.tissue.unit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.issue.application.dto.AddReviewerCommand;
import com.tissue.api.issue.presentation.controller.dto.request.AddReviewerRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueReviewerResponse;
import com.tissue.support.helper.ControllerTestHelper;

class ReviewControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces/{code}/issues/{issueKey}/reviewers - 리뷰어 추가에 성공하면 OK를 응답한다")
	void addReviewer_success_statusOk() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String issueKey = "ISSUE-1";
		Long reviewerMemberId = 1L;

		AddReviewerRequest request = new AddReviewerRequest(reviewerMemberId);
		AddReviewerCommand command = request.toCommand();

		IssueReviewerResponse response = IssueReviewerResponse.builder()
			.workspaceCode(workspaceCode)
			.issueKey(issueKey)
			.reviewerMemberId(reviewerMemberId)
			.build();

		// 서비스 모킹 - command 객체 매칭
		when(issueReviewerCommandService.addReviewer(
			eq(workspaceCode),
			eq(issueKey),
			eq(command),
			anyLong()
		)).thenReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/v1/workspaces/{code}/issues/{issueKey}/reviewers", workspaceCode, issueKey)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Reviewer added."))
			.andDo(print());
	}
}