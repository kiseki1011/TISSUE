package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueCreatedEvent extends IssueEvent {

	private final String issueTitle;

	public IssueCreatedEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId
	) {
		super(issue, triggeredByWorkspaceMemberId);
		this.issueTitle = issue.getTitle();
	}
}
