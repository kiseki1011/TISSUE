package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.JobType;
import com.uranus.taskmanager.api.member.domain.Member;

public record MyProfileResponse(
	String loginId,
	String email,
	String lastName,
	String firstName,
	LocalDate birthDate,
	JobType jobType,
	String introduction,
	int ownedWorkspaceCount,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static MyProfileResponse from(Member member) {
		return new MyProfileResponse(
			member.getLoginId(),
			member.getEmail(),
			member.getName().getLastName(),
			member.getName().getFirstName(),
			member.getBirthDate(),
			member.getJobType(),
			member.getIntroduction(),
			member.getMyWorkspaceCount(),
			member.getCreatedDate(),
			member.getLastModifiedDate()
		);
	}
}
