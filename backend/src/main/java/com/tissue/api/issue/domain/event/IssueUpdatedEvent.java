package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueUpdatedEvent extends IssueEvent {

	public IssueUpdatedEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId
	) {
		super(issue, triggeredByWorkspaceMemberId);
	}
}
