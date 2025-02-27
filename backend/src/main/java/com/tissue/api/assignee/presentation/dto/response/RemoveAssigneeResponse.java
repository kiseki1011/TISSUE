package com.tissue.api.assignee.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record RemoveAssigneeResponse(
	Long workspaceMemberId,
	String nickname
) {
	public static RemoveAssigneeResponse from(WorkspaceMember assignee) {
		return RemoveAssigneeResponse.builder()
			.workspaceMemberId(assignee.getId())
			.nickname(assignee.getNickname())
			.build();
	}
}
