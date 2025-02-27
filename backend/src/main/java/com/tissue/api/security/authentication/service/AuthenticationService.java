package com.tissue.api.security.authentication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberQueryService memberQueryService;
	private final MemberValidator memberValidator;

	@Transactional
	public LoginResponse login(LoginRequest request) {

		Member member = memberQueryService.findMember(request.identifier());

		memberValidator.validatePasswordMatch(request.password(), member.getPassword());

		return LoginResponse.from(member);
	}
}
