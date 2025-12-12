package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;

public interface IssueTransitionUseCase {

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE + OR + REQUIRES_PROJECT_ADMIN)
	void performTransition(PerformTransitionCommand cmd);
}
