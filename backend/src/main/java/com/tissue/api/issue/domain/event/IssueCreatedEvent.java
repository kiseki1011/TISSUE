package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;

import lombok.Getter;

@Getter
public class IssueCreatedEvent extends IssueEvent {

	public IssueCreatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId
	) {
		super(issueId, issueKey, workspaceCode, issueType, triggeredByWorkspaceMemberId);
	}

	public static IssueCreatedEvent createEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId
	) {
		return new IssueCreatedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId
		);
	}
}
