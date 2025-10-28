package com.tissue.api.issue.application.dto.response;

public record ParticipantInfo(
	Long memberId,
	String username,
	String displayName
	// String profilePic? (예정 중)
) {
}
