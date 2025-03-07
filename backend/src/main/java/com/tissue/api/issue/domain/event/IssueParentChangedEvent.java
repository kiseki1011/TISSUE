package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.types.Epic;

import lombok.Getter;

@Getter
public class IssueParentChangedEvent extends IssueEvent {

	private final Issue oldParent;
	private final Issue newParent;

	public IssueParentChangedEvent(
		Issue issue,
		Issue oldParent,
		Issue newParent,
		Long triggeredByWorkspaceMemberId
	) {
		super(issue, triggeredByWorkspaceMemberId);
		this.oldParent = oldParent;
		this.newParent = newParent;
	}

	public boolean hasEpicParentChanged() {
		return (oldParent instanceof Epic) || (newParent instanceof Epic);
	}
}
