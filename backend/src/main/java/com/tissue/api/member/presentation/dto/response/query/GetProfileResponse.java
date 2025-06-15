package com.tissue.api.member.presentation.dto.response.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;

import lombok.Builder;

@Builder
public record GetProfileResponse(
	String loginId,
	String email,
	String username,

	String name,
	LocalDate birthDate,
	JobType jobType,

	int ownedWorkspaceCount,

	LocalDateTime joinedAt,
	LocalDateTime lastModifiedAt
) {
	public static GetProfileResponse from(Member member) {
		return GetProfileResponse.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.username(member.getUsername())
			.name(member.getName())
			.birthDate(member.getBirthDate())
			.jobType(member.getJobType())
			.joinedAt(member.getCreatedDate())
			.lastModifiedAt(member.getLastModifiedDate())
			.ownedWorkspaceCount(member.getMyWorkspaceCount())
			.build();
	}
}
