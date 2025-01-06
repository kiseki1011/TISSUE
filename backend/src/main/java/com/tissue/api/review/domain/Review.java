package com.tissue.api.review.domain;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.review.exception.CannotChangeReviewStatusException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_REVIEWER_ID", nullable = false)
	private IssueReviewer issueReviewer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReviewStatus status;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private int reviewRound;

	@Builder
	public Review(
		IssueReviewer issueReviewer,
		ReviewStatus status,
		String title,
		String content,
		int reviewRound
	) {
		this.issueReviewer = issueReviewer;
		this.status = status;
		this.title = title;
		this.content = content;
		this.reviewRound = reviewRound;
	}

	public void updateStatus(ReviewStatus status) {
		validateIsPendingStatus();
		this.status = status;
	}

	private void validateIsPendingStatus() {
		if (this.status != ReviewStatus.PENDING) {
			throw new CannotChangeReviewStatusException();
		}
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public String getWorkspaceCode() {
		return issueReviewer.getReviewer().getWorkspaceCode();
	}
}
