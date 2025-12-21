package com.tissue.issue.application.port.in;

import static com.tissue.security.authorization.project.issue.IssueSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;

public interface IssueTransitionUseCase {

	@PreAuthorize(REQUIRES_ISSUE_EDITOR)
	void performTransition(PerformTransitionCommand cmd);
}
