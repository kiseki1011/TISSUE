package com.tissue.api.issue.presentation.dto.response.delete;

import java.time.LocalDateTime;

public record DeleteIssueResponse(

	Long issueId,
	String issueKey,
	LocalDateTime deletedAt

) {

	public static DeleteIssueResponse from(
		Long issueId,
		String issueKey,
		LocalDateTime deletedAt
	) {
		return new DeleteIssueResponse(issueId, issueKey, deletedAt);
	}
}
