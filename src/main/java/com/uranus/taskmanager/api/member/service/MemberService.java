package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.dto.response.SignupResponse;
import com.uranus.taskmanager.api.member.exception.DuplicateEmailException;
import com.uranus.taskmanager.api.member.exception.DuplicateLoginIdException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest signupRequest) {

		validateLoginIdAndEmail(signupRequest);
		Member member = createMember(signupRequest);

		return SignupResponse.from(memberRepository.save(member));
	}

	private Member createMember(SignupRequest signupRequest) {
		String encodedPassword = encodePassword(signupRequest.getPassword());

		// Todo: SignupRequest에 to()를 만들어서 사용하기
		return Member.builder()
			.loginId(signupRequest.getLoginId())
			.email(signupRequest.getEmail())
			.password(encodedPassword)
			.build();
	}

	private void validateLoginIdAndEmail(SignupRequest signupRequest) {
		checkLoginIdDuplicate(signupRequest.getLoginId());
		checkEmailDuplicate(signupRequest.getEmail());
	}

	private void checkLoginIdDuplicate(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new DuplicateLoginIdException();
		}
	}

	private void checkEmailDuplicate(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}
	}

	private String encodePassword(String password) {
		return passwordEncoder.encode(password);
	}

}
