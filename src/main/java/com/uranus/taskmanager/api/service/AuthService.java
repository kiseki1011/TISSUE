package com.uranus.taskmanager.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.member.Member;
import com.uranus.taskmanager.api.repository.MemberRepository;
import com.uranus.taskmanager.api.request.LoginRequest;
import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.response.LoginResponse;
import com.uranus.taskmanager.api.response.SignupResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;

	@Transactional
	public SignupResponse signup(SignupRequest signupRequest) {
		Member member = signupRequest.toEntity();
		// Todo: password 암호화
		return SignupResponse.fromEntity(memberRepository.save(member));
	}

	@Transactional
	public LoginResponse login(LoginRequest loginRequest) {
		/**
		 * Todo
		 * loginId, email 조회와 password 검증 분리
		 * password 암호화 로직 후에 검증 로직 수정
		 */
		Member member = memberRepository.findByLoginIdOrEmail(loginRequest.getLoginId(), loginRequest.getEmail())
			.stream()
			.filter(m -> m.getPassword().equals(loginRequest.getPassword()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Invalid login credentials or password"));

		return LoginResponse.fromEntity(member);
	}
}
