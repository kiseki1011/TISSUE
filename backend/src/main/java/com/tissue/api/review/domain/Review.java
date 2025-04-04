package com.tissue.api.review.domain;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

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

	@Column(nullable = false)
	private String workspaceCode;

	@Column
	private String issueKey;

	@Builder
	public Review(
		IssueReviewer issueReviewer,
		ReviewStatus status,
		String title,
		String content
	) {
		this.issueReviewer = issueReviewer;
		this.status = status;
		this.title = title;
		this.content = content;
		this.reviewRound = issueReviewer.getIssue().getCurrentReviewRound();
		this.workspaceCode = issueReviewer.getIssue().getWorkspaceCode();
		this.issueKey = issueReviewer.getIssue().getIssueKey();
	}

	// public void updateStatus(ReviewStatus status) {
	// 	validateCanUpdateStatus();
	// 	this.status = status;
	// }

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void validateIsAuthor(Long workspaceMemberId) {
		WorkspaceMember author = issueReviewer.getReviewer();

		if (!author.getId().equals(workspaceMemberId)) {
			throw new ForbiddenOperationException(
				String.format(
					"This review does not belong to the specified reviewer."
						+ " reviewId: %d, authorWorkspaceMemberId: %d, authorNickname: %s",
					id, author.getId(), author.getNickname()
				)
			);
		}
	}

	// private void validateCanUpdateStatus() {
	// 	boolean statusIsNotPending = status != ReviewStatus.COMMENT;
	//
	// 	if (statusIsNotPending) {
	// 		throw new InvalidOperationException(
	// 			String.format(
	// 				"Current review status must PENDING to update the review status. Current status: %s",
	// 				status
	// 			)
	// 		);
	// 	}
	// }
}
