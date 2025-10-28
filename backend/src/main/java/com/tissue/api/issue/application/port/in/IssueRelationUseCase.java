package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.response.IssueRelationResult;

public interface IssueRelationUseCase {
	IssueRelationResult add(AddIssueRelationCommand cmd);

	void remove(String workspaceKey, String sourceIssueKey, String targetIssueKey);
}
