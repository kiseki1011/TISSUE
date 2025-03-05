package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueStatusChangedEvent extends IssueEvent {

	public IssueStatusChangedEvent(Issue issue) {
		super(issue);
	}
}
