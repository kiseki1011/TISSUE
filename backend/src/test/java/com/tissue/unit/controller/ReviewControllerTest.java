package com.tissue.unit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ControllerTestHelper;

class ReviewControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces/{code}/issues/{issueKey}/reviewers/{workspaceMemberId} - 리뷰어 추가에 성공하면 OK를 응답한다")
	void addReviewer_success_statusOk() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String issueKey = "ISSUE-1";
		Long reviewerWorkspaceMemberId = 1L;

		AddReviewerResponse response = AddReviewerResponse.builder()
			.reviewerId(reviewerWorkspaceMemberId)
			.reviewerNickname("testNickname")
			.reviewerRole(WorkspaceRole.MEMBER)
			.build();

		when(reviewerCommandService.addReviewer(eq(workspaceCode), eq(issueKey), eq(reviewerWorkspaceMemberId),
			anyLong()))
			.thenReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/v1/workspaces/{code}/issues/{issueKey}/reviewers/{workspaceMemberId}", workspaceCode, issueKey,
					reviewerWorkspaceMemberId)
					.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Reviewer added."))
			.andExpect(jsonPath("$.data.reviewerId").value(1L))
			.andDo(print());
	}
}