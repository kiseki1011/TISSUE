package com.tissue.api.member.application.dto;

import java.time.LocalDate;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.member.domain.model.vo.Name;

import lombok.Builder;

@Builder
public record SignupMemberCommand(
	String loginId,
	String email,
	String username,
	String password,
	String firstName,
	String lastName,
	LocalDate birthDate,
	JobType jobType
) {
	public Member toEntity(String encodedPassword) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password(encodedPassword)
			.username(username)
			.name(Name.builder()
				.firstName(firstName)
				.lastName(lastName)
				.build())
			.birthDate(birthDate)
			.jobType(jobType)
			.build();
	}
}
