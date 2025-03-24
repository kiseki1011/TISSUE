package com.tissue.api.issue.domain.event;

import lombok.Getter;

@Getter
public class IssueUpdatedEvent extends IssueEvent {

	public IssueUpdatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId
	) {
		super(issueId, issueKey, workspaceCode, triggeredByWorkspaceMemberId);
	}
}
