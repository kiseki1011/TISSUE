package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

@Getter
public class IssueStoryPointChangedEvent extends IssueEvent {

	public IssueStoryPointChangedEvent(Issue issue) {
		super(issue);
	}
}
