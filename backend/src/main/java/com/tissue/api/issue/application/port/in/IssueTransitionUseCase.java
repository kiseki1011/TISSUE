package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;

public interface IssueTransitionUseCase {
	IssueCommandResult performTransition(PerformTransitionCommand cmd);
}
