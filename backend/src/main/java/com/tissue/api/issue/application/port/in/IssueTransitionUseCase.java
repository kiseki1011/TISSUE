package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueResponse;

public interface IssueTransitionUseCase {
	IssueResponse performTransition(PerformTransitionCommand cmd);
}
