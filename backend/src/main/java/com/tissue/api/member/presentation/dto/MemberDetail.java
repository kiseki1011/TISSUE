package com.tissue.api.member.presentation.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;

import lombok.Builder;

@Builder
public record MemberDetail(
	String loginId,
	String email,

	String name,
	LocalDate birthDate,
	JobType jobType,

	int ownedWorkspaceCount,

	Instant createdAt,
	Instant updatedAt
) {
	public static MemberDetail from(Member member) {
		return MemberDetail.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.name(member.getName())
			.birthDate(member.getBirthDate())
			.jobType(member.getJobType())
			.ownedWorkspaceCount(member.getWorkspaceCount())
			.createdAt(member.getCreatedAt())
			.updatedAt(member.getLastModifiedAt())
			.build();
	}
}
