package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueStoryPointChangedEvent extends IssueEvent {

	private final Integer oldStoryPoint;
	private final Integer newStoryPoint;

	public IssueStoryPointChangedEvent(
		Issue issue,
		Integer oldStoryPoint,
		Integer newStoryPoint,
		Long triggeredByWorkspaceMemberId
	) {
		super(issue, triggeredByWorkspaceMemberId);
		this.oldStoryPoint = oldStoryPoint;
		this.newStoryPoint = newStoryPoint;
	}

	public boolean hasStoryPointChanged() {
		return !newStoryPoint.equals(oldStoryPoint);
	}
}
