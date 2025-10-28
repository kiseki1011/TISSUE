package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueResult;

public interface IssueTransitionUseCase {
	IssueResult performTransition(PerformTransitionCommand cmd);
}
