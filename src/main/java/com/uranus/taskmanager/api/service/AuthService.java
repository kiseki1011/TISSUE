package com.uranus.taskmanager.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.member.Member;
import com.uranus.taskmanager.api.exception.InvalidLoginIdentityException;
import com.uranus.taskmanager.api.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.repository.MemberRepository;
import com.uranus.taskmanager.api.request.LoginRequest;
import com.uranus.taskmanager.api.response.LoginResponse;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public LoginResponse login(LoginRequest loginRequest) {

		Member member = memberRepository.findByLoginIdOrEmail(loginRequest.getLoginId(), loginRequest.getEmail())
			.orElseThrow(InvalidLoginIdentityException::new);

		if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
			throw new InvalidLoginPasswordException();
		}
		return LoginResponse.fromEntity(member);
	}
}
