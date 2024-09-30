package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.member.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.dto.response.SignupResponse;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest signupRequest) {
		// 로그인 ID, 이메일 중복 체크
		checkLoginIdDuplicate(signupRequest.getLoginId());
		checkEmailDuplicate(signupRequest.getEmail());

		String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

		Member member = Member.builder()
			.loginId(signupRequest.getLoginId())
			.email(signupRequest.getEmail())
			.password(encodedPassword)
			.build();
		return SignupResponse.fromEntity(memberRepository.save(member));
	}

	public void checkLoginIdDuplicate(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new IllegalArgumentException("Login ID already exists."); // Todo: DuplicateLoginIdException 만들기
		}
	}

	public void checkEmailDuplicate(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already exists."); // Todo: DuplicateEmailException 만들기
		}
	}
}
