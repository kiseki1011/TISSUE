package com.tissue.issue.application.port.in;

import static com.tissue.issue.application.service.authorization.IssueAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;

public interface IssueTransitionUseCase {

	@PreAuthorize(REQUIRES_ISSUE_EDIT_PERMISSION)
	void performTransition(PerformTransitionCommand cmd);
}
