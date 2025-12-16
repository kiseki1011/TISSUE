package com.tissue.issue.application.dto.response.info;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.enums.IssuePriority;

public record RelatedIssueInfo(
	String issueKey,
	String title,
	String stateDisplayLabel,
	IssuePriority priority
) {
	public static RelatedIssueInfo from(Issue issue) {
		return new RelatedIssueInfo(
			issue.getKey(),
			issue.getTitle(),
			issue.getCurrentState().getDisplayLabel(),
			issue.getPriority()
		);
	}
}
