package com.tissue.api.issue.application.dto.response.info;

import com.tissue.api.workspace.domain.WorkspaceMember;

public record ParticipantInfo(
	Long memberId,
	String username,
	String displayName,
	boolean archived
	// String profileImg (예정 중)
) {
	public static ParticipantInfo from(WorkspaceMember wm) {
		if (wm == null) {
			return null;
		}
		return new ParticipantInfo(
			wm.getMember().getId(),
			wm.getMember().getUsername(),
			wm.getDisplayName(),
			wm.isArchived()
		);
	}
}
