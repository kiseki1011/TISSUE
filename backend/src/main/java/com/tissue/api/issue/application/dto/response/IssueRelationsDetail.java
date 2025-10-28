package com.tissue.api.issue.application.dto.response;

import java.util.List;

import com.tissue.api.issue.domain.Issue;

public record IssueRelationsDetail(
	List<RelatedIssueInfo> blocks,
	List<RelatedIssueInfo> blockedBy,
	List<RelatedIssueInfo> duplicates,
	List<RelatedIssueInfo> duplicatedBy,
	List<RelatedIssueInfo> relevant
) {
	public static IssueRelationsDetail from(Issue issue) {
		return new IssueRelationsDetail(
			issue.getRelations().getBlockingIssues().stream()
				.map(RelatedIssueInfo::from)
				.toList(),
			issue.getRelations().getBlockedByIssues().stream()
				.map(RelatedIssueInfo::from)
				.toList(),
			issue.getRelations().getDuplicates().stream()
				.map(RelatedIssueInfo::from)
				.toList(),
			issue.getRelations().getDuplicatedBy().stream()
				.map(RelatedIssueInfo::from)
				.toList(),
			issue.getRelations().getRelevantIssues().stream()
				.map(RelatedIssueInfo::from)
				.toList()
		);
	}
}
