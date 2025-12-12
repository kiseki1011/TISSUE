package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.request.RemoveIssueRelationCommand;

public interface IssueRelationUseCase {

	// TODO: 응답을 IssueCommandResult vs IssueRelationResult 중 뭘 사용하는게 좋을까?
	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	void add(AddIssueRelationCommand cmd);

	// TODO: RemoveIssueRelationCommand
	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	void remove(RemoveIssueRelationCommand cmd);
}
