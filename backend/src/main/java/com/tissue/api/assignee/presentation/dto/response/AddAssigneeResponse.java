package com.tissue.api.assignee.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record AddAssigneeResponse(
	Long workspaceMemberId,
	String nickname
) {
	public static AddAssigneeResponse from(WorkspaceMember assignee) {
		return AddAssigneeResponse.builder()
			.workspaceMemberId(assignee.getId())
			.nickname(assignee.getNickname())
			.build();
	}
}
