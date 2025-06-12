package com.tissue.api.member.application.dto;

import java.time.LocalDate;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;

import lombok.Builder;

@Builder
public record SignupMemberCommand(
	String loginId,
	String email,
	String username,
	String password,
	String name,
	LocalDate birthDate,
	JobType jobType
) {
	public Member toEntity(String encodedPassword) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password(encodedPassword)
			.username(username)
			.name(name)
			.birthDate(birthDate)
			.jobType(jobType)
			.build();
	}
}
