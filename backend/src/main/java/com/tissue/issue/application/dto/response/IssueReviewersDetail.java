package com.tissue.issue.application.dto.response;

import java.util.List;

import com.tissue.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.issue.domain.IssueReviewer;

public record IssueReviewersDetail(
	List<ParticipantInfo> reviewers,
	int totalCount
) {
	public static IssueReviewersDetail from(List<IssueReviewer> reviewers) {
		return new IssueReviewersDetail(
			reviewers.stream()
				.map(IssueReviewer::getReviewer)
				.map(ParticipantInfo::from)
				.toList(),
			reviewers.size()
		);
	}
}
