package com.tissue.api.review.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.enums.ReviewStatus;

import lombok.Builder;

@Builder
public record SubmitReviewResponse(
	Long reviewId,

	ReviewStatus status,
	String content,
	int reviewRound,

	Long reviewerId,
	String reviewerNickName,

	LocalDateTime createdAt
) {

	public static SubmitReviewResponse from(Review review) {
		return SubmitReviewResponse.builder()
			.reviewId(review.getId())
			.status(review.getStatus())
			.content(review.getContent())
			.reviewRound(review.getReviewRound())
			.reviewerId(review.getIssueReviewer().getReviewer().getId())
			.reviewerNickName(
				review.getIssueReviewer().getReviewer().getDisplayName()) // Todo: 최적화 여지, 서비스의 WorkspaceMember에서 가져오기
			.createdAt(review.getCreatedDate())
			.build();
	}
}
