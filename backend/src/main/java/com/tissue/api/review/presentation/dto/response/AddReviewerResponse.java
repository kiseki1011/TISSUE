package com.tissue.api.review.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Builder;

/**
 * The response for adding a reviewer
 *
 * @param reviewerId - Must be a WORKSPACE_MEMBER_ID
 * @param reviewerNickname
 * @param reviewerRole
 */
@Builder
public record AddReviewerResponse(
	Long reviewerId,
	String reviewerNickname,
	WorkspaceRole reviewerRole
) {
	public static AddReviewerResponse from(WorkspaceMember reviewer) {
		return AddReviewerResponse.builder()
			.reviewerId(reviewer.getId())
			.reviewerNickname(reviewer.getNickname())
			.reviewerRole(reviewer.getRole())
			.build();
	}
}
