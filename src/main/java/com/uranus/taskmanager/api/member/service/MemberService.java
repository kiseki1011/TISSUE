package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberEmailUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberPasswordUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberWithdrawRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MemberEmailUpdateResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.SignupResponse;
import com.uranus.taskmanager.api.member.validator.MemberValidator;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	private final MemberValidator memberValidator;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		memberValidator.validateSignup(request);

		Member member = createMember(request);

		return SignupResponse.from(memberRepository.save(member));
	}

	@Transactional
	public MemberEmailUpdateResponse updateEmail(MemberEmailUpdateRequest request, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		String toBeEmail = request.getUpdateEmail();
		memberValidator.validateEmailUpdate(toBeEmail);

		member.updateEmail(toBeEmail);

		return MemberEmailUpdateResponse.from(member);
	}

	@Transactional
	public void updatePassword(MemberPasswordUpdateRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		String toBePassword = encodePassword(request.getUpdatePassword());

		member.updatePassword(toBePassword);
	}

	@Transactional
	public void withdrawMember(MemberWithdrawRequest request, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		memberValidator.validatePassword(request.getPassword(), member.getPassword());
		memberValidator.validateWithdraw(memberId);

		memberRepository.delete(member);
	}

	private Member createMember(SignupRequest request) {
		String encodedPassword = encodePassword(request.getPassword());

		// Todo: 그냥 빌더 사용 고려, SignupRequest의 toMember 제거
		return SignupRequest.toMember(request, encodedPassword);
	}

	private String encodePassword(String password) {
		return passwordEncoder.encode(password);
	}
}
