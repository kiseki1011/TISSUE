package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.response.IssueCommandResult;

public interface IssueParticipantUseCase {
	IssueCommandResult changeReporter(String workspaceKey, String issueKey, Long memberId);

	IssueCommandResult assignTo(String workspaceKey, String issueKey, Long memberId);

	IssueCommandResult unassign(String workspaceKey, String issueKey);

	IssueCommandResult subscribe(String workspaceKey, String issueKey, Long memberId);

	IssueCommandResult unsubscribe(String workspaceKey, String issueKey, Long memberId);

	IssueCommandResult addReviewer(String workspaceKey, String issueKey, Long memberId);

	IssueCommandResult removeReviewer(String workspaceKey, String issueKey, Long memberId);
}
