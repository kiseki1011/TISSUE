package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class IssueEvent {
	private final Issue issue;
	private final Long triggeredByWorkspaceMemberId;
}
