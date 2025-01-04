package com.tissue.api.review.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record RequestReviewResponse(
	int currentReviewRound,
	LocalDateTime reviewRequestedAt,
	List<ReviewerDetail> reviewerDetails
) {

	public static RequestReviewResponse from(Issue issue) {
		return RequestReviewResponse.builder()
			.currentReviewRound(issue.getCurrentReviewRound())
			.reviewRequestedAt(issue.getLastModifiedDate()) // Todo: reviewRequestedAt 추가되면 변경
			.reviewerDetails(issue.getReviewers().stream()
				.map(ReviewerDetail::from)
				.toList())
			.build();
	}
}
