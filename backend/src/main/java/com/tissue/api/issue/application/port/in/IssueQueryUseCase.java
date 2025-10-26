package com.tissue.api.issue.application.port.in;

import java.util.List;

import com.tissue.api.issue.adapter.in.web.dto.response.IssueDetailResponse;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;

public interface IssueQueryUseCase {

	IssueDetailResponse getDetailedIssue(String workspaceKey, String issueKey);

	List<TransitionResponse> getAvailableTransitions(String workspaceKey, String issueKey);
	// List<TransitionDto> getAvailableTransitions(String workspaceKey, String issueKey);
}
