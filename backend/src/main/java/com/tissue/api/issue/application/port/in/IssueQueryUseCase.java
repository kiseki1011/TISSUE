package com.tissue.api.issue.application.port.in;

import java.util.List;

import com.tissue.api.issue.application.dto.response.IssueDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;

public interface IssueQueryUseCase {

	IssueDetail getIssueDetails(String workspaceKey, String issueKey);

	List<TransitionDetail> getAvailableTransitions(String workspaceKey, String issueKey);
	// List<TransitionDto> getAvailableTransitions(String workspaceKey, String issueKey);
}
