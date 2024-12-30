package com.tissue.fixture.dto;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;

@Component
public class SignupRequestDtoFixture {

	public SignupMemberRequest createSignupRequest(
		String loginId,
		String email,
		String password
	) {
		return SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();
	}
}
