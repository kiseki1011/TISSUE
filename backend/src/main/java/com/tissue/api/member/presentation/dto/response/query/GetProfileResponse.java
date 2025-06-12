package com.tissue.api.member.presentation.dto.response.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;

public record GetProfileResponse(
	String loginId,
	String email,

	String name,
	LocalDate birthDate,
	JobType jobType,

	int ownedWorkspaceCount,

	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static GetProfileResponse from(Member member) {
		return new GetProfileResponse(
			member.getLoginId(),
			member.getEmail(),
			member.getName(),
			member.getBirthDate(),
			member.getJobType(),
			member.getMyWorkspaceCount(),
			member.getCreatedDate(),
			member.getLastModifiedDate()
		);
	}
}
