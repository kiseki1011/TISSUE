package com.tissue.api.review.domain;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueReviewer extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_ID", nullable = false)
	private Issue issue;

	@Column(name = "REVIEWER_ID")
	private Long reviewerMemberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REVIEWER_ID", insertable = false, updatable = false)
	private WorkspaceMember reviewer;

	@OneToMany(mappedBy = "issueReviewer", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<Review> reviews = new ArrayList<>();

	public IssueReviewer(WorkspaceMember reviewer, Issue issue) {
		this.reviewer = reviewer;
		this.reviewerMemberId = reviewer.getMember().getId();
		this.issue = issue;
	}

	public Review submitReview(
		ReviewStatus status,
		String title,
		String content
	) {
		// 해당 라운드에 이미 리뷰가 있는지 확인
		validateNoReviewInRound(issue.getCurrentReviewRound());

		Review review = Review.builder()
			.issueReviewer(this)
			.status(status)
			.title(title)
			.content(content)
			.build();
		this.reviews.add(review);

		return review;
	}

	public boolean hasReviewForRound(int reviewRound) {
		return reviews.stream()
			.anyMatch(review -> review.getReviewRound() == reviewRound);
	}

	public ReviewStatus getCurrentReviewStatus(int reviewRound) {
		return reviews.stream()
			.filter(review -> review.getReviewRound() == reviewRound)
			.map(Review::getStatus)
			.findFirst()
			.orElse(ReviewStatus.COMMENT);
	}

	private void validateNoReviewInRound(int reviewRound) {
		boolean hasReviewInRound = reviews.stream()
			.anyMatch(review -> review.getReviewRound() == reviewRound);

		if (hasReviewInRound) {
			throw new InvalidOperationException(
				String.format("Reviewer already has a review for this round. review round: %d", reviewRound)
			);
		}
	}
}
