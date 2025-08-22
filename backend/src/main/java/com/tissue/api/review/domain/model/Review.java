package com.tissue.api.review.domain.model;

import com.tissue.api.common.entity.BaseEntity;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	// private Long id;
	//
	// @ManyToOne(fetch = FetchType.LAZY)
	// @JoinColumn(name = "ISSUE_REVIEWER_ID", nullable = false)
	// private IssueReviewer issueReviewer;
	//
	// @Enumerated(EnumType.STRING)
	// @Column(nullable = false)
	// private ReviewStatus status;
	//
	// @Column(nullable = false)
	// private String title;
	//
	// @Lob
	// @Column(nullable = false)
	// private String content;
	//
	// @Column(nullable = false)
	// private int reviewRound;
	//
	// @Column(nullable = false)
	// private String workspaceKey;
	//
	// @Column
	// private String issueKey;
	//
	// @Builder
	// public Review(
	// 	IssueReviewer issueReviewer,
	// 	ReviewStatus status,
	// 	String title,
	// 	String content
	// ) {
	// 	this.issueReviewer = issueReviewer;
	// 	this.status = status;
	// 	this.title = title;
	// 	this.content = content;
	// 	this.reviewRound = issueReviewer.getIssue().getCurrentReviewRound();
	// 	this.workspaceKey = issueReviewer.getIssue().getWorkspaceCode();
	// 	this.issueKey = issueReviewer.getIssue().getIssueKey();
	// }
	//
	// public static Review create(
	// 	IssueReviewer reviewer,
	// 	ReviewStatus status,
	// 	String title,
	// 	String content
	// ) {
	// 	return Review.builder()
	// 		.issueReviewer(reviewer)
	// 		.status(status)
	// 		.title(title)
	// 		.content(content)
	// 		.build();
	// }
	//
	// public void updateTitle(String title) {
	// 	this.title = title;
	// }
	//
	// public void updateContent(String content) {
	// 	this.content = content;
	// }
	//
	// public void validateIsAuthor(Long workspaceMemberId) {
	// 	WorkspaceMember author = issueReviewer.getReviewer();
	//
	// 	if (!author.getId().equals(workspaceMemberId)) {
	// 		throw new ForbiddenOperationException(
	// 			String.format(
	// 				"This review does not belong to the specified reviewer."
	// 					+ " reviewId: %d, authorWorkspaceMemberId: %d, authorNickname: %s",
	// 				id, author.getId(), author.getDisplayName()
	// 			)
	// 		);
	// 	}
	// }
}
