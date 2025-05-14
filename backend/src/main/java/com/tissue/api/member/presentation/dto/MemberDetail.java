package com.tissue.api.member.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;

import lombok.Builder;

@Builder
public record MemberDetail(
	String loginId,
	String email,

	String lastName,
	String firstName,
	LocalDate birthDate,
	JobType jobType,
	String biography,

	int ownedWorkspaceCount,

	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static MemberDetail from(Member member) {
		return MemberDetail.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.lastName(member.getName().getLastName())
			.firstName(member.getName().getFirstName())
			.birthDate(member.getBirthDate())
			.jobType(member.getJobType())
			.biography(member.getBiography())
			.ownedWorkspaceCount(member.getMyWorkspaceCount())
			.createdAt(member.getCreatedDate())
			.updatedAt(member.getLastModifiedDate())
			.build();
	}
}
