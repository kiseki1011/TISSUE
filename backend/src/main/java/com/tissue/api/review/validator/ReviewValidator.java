package com.tissue.api.review.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.exception.IssueStatusNotInReviewException;
import com.tissue.api.review.exception.UnauthorizedReviewAccessException;
import com.tissue.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewValidator {

	public void validateReviewIsCreateable(Issue issue) {
		if (issue.getStatus() != IssueStatus.IN_REVIEW) {
			throw new IssueStatusNotInReviewException(); // Todo: InvalidIssueStatusException로 변경(메세지로 세부 사항 전달)
		}
	}

	public void validateReviewOwnership(Review review, Long reviewerWorkspaceMemberId) {
		if (!review.getIssueReviewer().getReviewer().getId().equals(reviewerWorkspaceMemberId)) {
			throw new UnauthorizedReviewAccessException(
				"This review does not belong to the specified reviewer."
			);
		}
	}

	public void validateRoleIsLowerThanMember(WorkspaceMember reviewer) {
		if (reviewer.getRole().isLowerThan(WorkspaceRole.MEMBER)) {
			throw new InsufficientWorkspaceRoleException(
				String.format(
					"Reviewer must have at least workspace role MEMBER. Current workspace role: %s",
					reviewer.getRole()
				)
			);
		}
	}
}
