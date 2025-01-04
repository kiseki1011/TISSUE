package com.tissue.api.review.domain;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.review.exception.DuplicateReviewInRoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.CascadeType;
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
public class IssueReviewer extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REVIEWER_ID", nullable = false)
	private WorkspaceMember reviewer;

	@OneToMany(mappedBy = "issueReviewer", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<Review> reviews = new ArrayList<>();

	public IssueReviewer(WorkspaceMember reviewer) {
		this.reviewer = reviewer;
	}

	public void addReview(ReviewStatus status, String comment, int reviewRound) {
		// 해당 라운드에 이미 리뷰가 있는지 확인
		boolean hasReviewInRound = reviews.stream()
			.anyMatch(review -> review.getReviewRound() == reviewRound);

		if (hasReviewInRound) {
			throw new DuplicateReviewInRoundException();
		}

		Review review = new Review(this, status, comment, reviewRound);
		this.reviews.add(review);
	}

	public ReviewStatus getCurrentReviewStatus(int reviewRound) {
		return reviews.stream()
			.filter(review -> review.getReviewRound() == reviewRound)
			.map(Review::getStatus)
			.findFirst()
			.orElse(ReviewStatus.PENDING);
	}
}
