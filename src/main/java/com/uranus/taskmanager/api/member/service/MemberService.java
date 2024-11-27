package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.SignupMemberResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.UpdateMemberEmailResponse;
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
	public SignupMemberResponse signup(SignupMemberRequest request) {
		memberValidator.validateSignup(request);

		String encodedPassword = passwordEncoder.encode(request.getPassword());

		Member member = SignupMemberRequest.to(request, encodedPassword);

		return SignupMemberResponse.from(memberRepository.save(member));
	}

	@Transactional
	public UpdateMemberEmailResponse updateEmail(UpdateMemberEmailRequest request, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		String previousEmail = member.getEmail();

		String toBeEmail = request.getUpdateEmail();
		memberValidator.validateEmailUpdate(toBeEmail);

		member.updateEmail(toBeEmail);

		return UpdateMemberEmailResponse.from(member, previousEmail);
	}

	@Transactional
	public void updatePassword(UpdateMemberPasswordRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		String toBePassword = passwordEncoder.encode(request.getUpdatePassword());

		member.updatePassword(toBePassword);
	}

	@Transactional
	public void withdraw(WithdrawMemberRequest request, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		memberValidator.validatePassword(request.getPassword(), member.getPassword());
		memberValidator.validateWithdraw(memberId);

		memberRepository.delete(member);
	}

}
