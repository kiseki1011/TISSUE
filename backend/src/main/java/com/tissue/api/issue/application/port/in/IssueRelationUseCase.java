package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.response.IssueRelationResponse;

public interface IssueRelationUseCase {
	IssueRelationResponse add(AddIssueRelationCommand cmd);

	void remove(String workspaceKey, String sourceIssueKey, String targetIssueKey);
}
