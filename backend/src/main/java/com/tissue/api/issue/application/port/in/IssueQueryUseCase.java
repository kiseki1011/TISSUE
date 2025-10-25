package com.tissue.api.issue.application.port.in;

import java.util.List;

import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;

public interface IssueQueryUseCase {

	IssueDetailDto getDetailedIssue(String workspaceKey, String issueKey);

	List<TransitionResponse> getAvailableTransitions(String workspaceKey, String issueKey);
	// List<TransitionDto> getAvailableTransitions(String workspaceKey, String issueKey);
}
