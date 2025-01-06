package com.tissue.api.review.presentation.dto.response;

import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Builder;

@Builder
public record ReviewerDetail(
	Long workspaceMemberId,
	String nickname,
	WorkspaceRole role
) {
	public static ReviewerDetail from(IssueReviewer reviewer) {
		return ReviewerDetail.builder()
			.workspaceMemberId(reviewer.getReviewer().getId())
			.nickname(reviewer.getReviewer().getNickname())
			.role(reviewer.getReviewer().getRole())
			.build();
	}
}
