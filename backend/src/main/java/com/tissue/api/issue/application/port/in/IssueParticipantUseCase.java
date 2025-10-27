package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.response.IssueResponse;

public interface IssueParticipantUseCase {
	IssueResponse changeReporter(String workspaceKey, String issueKey, Long memberId);

	IssueResponse assignTo(String workspaceKey, String issueKey, Long memberId);

	IssueResponse unassign(String workspaceKey, String issueKey);

	IssueResponse subscribe(String workspaceKey, String issueKey, Long memberId);

	IssueResponse unsubscribe(String workspaceKey, String issueKey, Long memberId);

	IssueResponse addReviewer(String workspaceKey, String issueKey, Long memberId);

	IssueResponse removeReviewer(String workspaceKey, String issueKey, Long memberId);
}
