package com.tissue.api.comment.domain.model;

import com.tissue.api.review.domain.model.Review;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("REVIEW_COMMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewComment extends Comment {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REVIEW_ID", nullable = false)
	private Review review;

	/*
	 * Todo
	 *  - 깃허브 API 연동
	 *  - line number, PR number, github comment id, 등을 추가
	 */

	@Builder
	private ReviewComment(
		String content,
		WorkspaceMember author,
		Review review,
		Comment parentComment
	) {
		super(content, author, parentComment);
		this.review = review;
	}
}
