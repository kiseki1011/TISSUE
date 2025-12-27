package com.tissue.issue.application.dto.response.info;

import com.tissue.issue.domain.Issue;

public record IssueIdentificationInfo(
	String issueKey,
	String issueTypeLabel
) {
	public static IssueIdentificationInfo from(Issue issue) {
		return new IssueIdentificationInfo(issue.getKey(), issue.getIssueType().getDisplayName());
	}

	public static IssueIdentificationInfo asNull() {
		return new IssueIdentificationInfo(null, null);
	}
}
