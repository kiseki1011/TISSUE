package com.tissue.api.issue.application.port.in;

import static com.tissue.api.security.authorization.IssueSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;

public interface IssueTransitionUseCase {

	@PreAuthorize(REQUIRES_ISSUE_EDITOR)
	void performTransition(PerformTransitionCommand cmd);
}
