package com.uranus.taskmanager.api.authentication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginRequest;
import com.uranus.taskmanager.api.authentication.dto.response.LoginResponse;
import com.uranus.taskmanager.api.authentication.exception.InvalidLoginIdentityException;
import com.uranus.taskmanager.api.authentication.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public LoginResponse login(LoginRequest loginRequest) {

		Member member = memberRepository.findByLoginIdOrEmail(loginRequest.getLoginId(), loginRequest.getEmail())
			.orElseThrow(InvalidLoginIdentityException::new);

		if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
			throw new InvalidLoginPasswordException();
		}
		return LoginResponse.from(member);
	}
}
