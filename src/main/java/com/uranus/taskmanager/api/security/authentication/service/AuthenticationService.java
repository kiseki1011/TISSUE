package com.uranus.taskmanager.api.security.authentication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.security.authentication.presentation.dto.request.LoginRequest;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.response.LoginResponse;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginIdentityException;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public LoginResponse login(LoginRequest loginRequest) {

		Member member = findMemberByLoginIdOrEmail(loginRequest);

		validatePassword(loginRequest, member);

		return LoginResponse.from(member);
	}

	private Member findMemberByLoginIdOrEmail(LoginRequest loginRequest) {
		return memberRepository.findByLoginIdOrEmail(loginRequest.getLoginId(), loginRequest.getEmail())
			.orElseThrow(InvalidLoginIdentityException::new);
	}

	private void validatePassword(LoginRequest loginRequest, Member member) {
		if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
			throw new InvalidLoginPasswordException();
		}
	}
}
