package com.tissue.api.issue.application.port.in;

import java.util.List;

import com.tissue.api.issue.application.dto.response.IssueCommonFieldsDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;

public interface IssueQueryUseCase {

	IssueCommonFieldsDetail getIssueDetails(String workspaceKey, String issueKey);

	List<TransitionDetail> getAvailableTransitions(String workspaceKey, String issueKey);
}
