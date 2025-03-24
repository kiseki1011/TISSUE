package com.tissue.api.issue.domain.event;

import lombok.Getter;

@Getter
public class IssueCreatedEvent extends IssueEvent {

	public IssueCreatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId
	) {
		super(issueId, issueKey, workspaceCode, triggeredByWorkspaceMemberId);
	}
}
