package com.tissue.api.issue.application.dto.response.info;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

public record ParticipantInfo(
	Long memberId,
	String username,
	String displayName,
	boolean archived
	// String profilePic? (예정 중)
) {
	public static ParticipantInfo from(WorkspaceMember wm) {
		return new ParticipantInfo(
			wm.getMember().getId(),
			wm.getMember().getUsername(),
			wm.getDisplayName(),
			wm.isArchived()
		);
	}
}
