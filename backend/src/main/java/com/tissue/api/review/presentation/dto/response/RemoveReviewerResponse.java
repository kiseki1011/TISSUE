package com.tissue.api.review.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record RemoveReviewerResponse(
	Long workspaceMemberId,
	String nickname,
	LocalDateTime removedAt
) {
	public static RemoveReviewerResponse from(WorkspaceMember reviewer, Issue issue) {
		return RemoveReviewerResponse.builder()
			.workspaceMemberId(reviewer.getId())
			.nickname(reviewer.getNickname())
			.removedAt(issue.getLastModifiedDate())
			.build();
	}
}
