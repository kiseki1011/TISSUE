package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;

import lombok.Getter;

@Getter
public class IssueStatusChangedEvent extends IssueEvent {

	private final IssueStatus oldStatus;
	private final IssueStatus newStatus;

	public IssueStatusChangedEvent(
		Issue issue,
		IssueStatus oldStatus,
		IssueStatus newStatus,
		Long triggeredByWorkspaceMemberId
	) {
		super(issue, triggeredByWorkspaceMemberId);
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public boolean isClosedStatusChange() {
		return oldStatus != newStatus && (oldStatus == IssueStatus.CLOSED || newStatus == IssueStatus.CLOSED);
	}
}
