package com.tissue.api.issue.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;

@Transactional
public interface IssueTransitionUseCase {
	IssueCommandResult performTransition(PerformTransitionCommand cmd);
}
