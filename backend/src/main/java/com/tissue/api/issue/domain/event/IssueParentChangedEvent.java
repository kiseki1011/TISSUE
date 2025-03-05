package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueParentChangedEvent extends IssueEvent {

	public IssueParentChangedEvent(Issue issue) {
		super(issue);
	}
}
