package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.response.IssueResult;

public interface IssueParticipantUseCase {
	IssueResult changeReporter(String workspaceKey, String issueKey, Long memberId);

	IssueResult assignTo(String workspaceKey, String issueKey, Long memberId);

	IssueResult unassign(String workspaceKey, String issueKey);

	IssueResult subscribe(String workspaceKey, String issueKey, Long memberId);

	IssueResult unsubscribe(String workspaceKey, String issueKey, Long memberId);

	IssueResult addReviewer(String workspaceKey, String issueKey, Long memberId);

	IssueResult removeReviewer(String workspaceKey, String issueKey, Long memberId);
}
