package com.tissue.api.review.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueReviewerValidator {

	public void validateReviewer(WorkspaceMember reviewer) {
		validateRoleIsLowerThanMember(reviewer);
	}

	private void validateRoleIsLowerThanMember(WorkspaceMember reviewer) {
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
