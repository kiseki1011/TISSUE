package com.tissue.fixture.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.member.service.command.MemberCommandService;

@Component
public class MemberFixture {

	@Autowired
	private MemberCommandService memberCommandService;

	public SignupMemberResponse createMember(String loginId, String email) {
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password("test1234!")
			.biography("Im a test user!")
			.jobType(JobType.DEVELOPER)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.parse("1900-10-10"))
			.build();

		return memberCommandService.signup(request);
	}
}
