package com.tissue.api.security.authentication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.security.authentication.exception.InvalidLoginIdentityException;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;

	@Transactional
	public LoginResponse login(LoginRequest request) {

		Member member = findMemberByLoginIdOrEmail(request);

		memberValidator.validatePasswordMatch(request.getPassword(), member.getPassword());

		return LoginResponse.from(member);
	}

	private Member findMemberByLoginIdOrEmail(LoginRequest request) {
		return memberRepository.findByLoginIdOrEmail(request.getLoginId(), request.getEmail())
			.orElseThrow(InvalidLoginIdentityException::new);
	}
}
