package com.uranus.taskmanager.api.member.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.domain.vo.Name;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberNameRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.SignupMemberResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.UpdateMemberEmailResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.UpdateMemberInfoResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.UpdateMemberNameResponse;
import com.uranus.taskmanager.api.member.validator.MemberValidator;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Todo
	 *  - 회원 가입도 동시성 관련 처리 필요
	 */
	@Transactional
	public SignupMemberResponse signup(
		SignupMemberRequest request
	) {
		memberValidator.validateSignup(request);

		Member member = createMember(request);
		Member savedMember = memberRepository.save(member);

		return SignupMemberResponse.from(savedMember);
	}

	@Transactional
	public UpdateMemberInfoResponse updateInfo(
		UpdateMemberInfoRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		updateMemberInfoIfPresent(request, member);

		return UpdateMemberInfoResponse.from(member);
	}

	public UpdateMemberNameResponse updateName(
		UpdateMemberNameRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		member.updateName(Name.builder()
			.firstName(request.getFirstName())
			.lastName(request.getLastName())
			.build());

		return UpdateMemberNameResponse.from(member);
	}

	@Transactional
	public UpdateMemberEmailResponse updateEmail(
		UpdateMemberEmailRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		String newEmail = request.getNewEmail();
		memberValidator.validateEmailUpdate(newEmail);

		member.updateEmail(newEmail);

		return UpdateMemberEmailResponse.from(member);
	}

	@Transactional
	public void updatePassword(
		UpdateMemberPasswordRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		String encodedNewPassword = encodePassword(request.getNewPassword());

		member.updatePassword(encodedNewPassword);
	}

	@Transactional
	public void withdraw(
		WithdrawMemberRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		validateMemberDeletion(request, memberId, member);

		memberRepository.delete(member);
	}

	private void updateMemberInfoIfPresent(UpdateMemberInfoRequest request, Member member) {

		if (request.hasBirthDate()) {
			member.updateBirthDate(request.getBirthDate());
		}
		if (request.hasJobType()) {
			member.updateJobType(request.getJobType());
		}
		if (request.hasIntroduction()) {
			member.updateIntroduction(request.getIntroduction());
		}
	}

	private Member findMemberById(
		Long memberId
	) {
		return memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);
	}

	private Member createMember(
		SignupMemberRequest request
	) {
		String encodedPassword = encodePassword(request.getPassword());
		return request.toEntity(encodedPassword);
	}

	private String encodePassword(
		String rawPassword
	) {
		return passwordEncoder.encode(rawPassword);
	}

	private void validateMemberDeletion(
		WithdrawMemberRequest request,
		Long memberId,
		Member member
	) {
		memberValidator.validatePassword(request.getPassword(), member.getPassword());
		memberValidator.validateWithdraw(memberId);
	}
}
