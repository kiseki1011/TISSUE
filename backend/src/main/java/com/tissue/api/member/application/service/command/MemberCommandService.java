package com.tissue.api.member.application.service.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.member.application.dto.SignupMemberCommand;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberProfileRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberUsernameRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

	private final MemberReader memberReader;
	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final MemberEmailVerificationService memberEmailVerificationService;

	@Transactional
	public MemberResponse signup(
		SignupMemberCommand command
	) {
		memberValidator.validateLoginIdIsUnique(command.loginId());
		memberValidator.validateEmailIsUnique(command.email());
		memberValidator.validateUsernameIsUnique(command.username());

		memberEmailVerificationService.validateEmailVerified(command.email());

		String encodedPassword = passwordEncoder.encode(command.password());
		Member member = command.toEntity(encodedPassword);

		try {
			Member savedMember = memberRepository.save(member);
			memberEmailVerificationService.clearVerification(command.email());

			return MemberResponse.from(savedMember);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("Failed to signup.", e);
		}
	}

	@Transactional
	public MemberResponse updateInfo(
		UpdateMemberProfileRequest request,
		Long memberId
	) {
		Member member = memberReader.findMemberById(memberId);

		updateMemberInfoIfPresent(request, member);

		return MemberResponse.from(member);
	}

	@Transactional
	public MemberResponse updateEmail(
		UpdateMemberEmailRequest request,
		Long memberId
	) {
		Member member = memberReader.findMemberById(memberId);

		memberValidator.validateEmailIsUnique(request.newEmail());
		memberEmailVerificationService.validateEmailVerified(request.newEmail());

		try {
			member.updateEmail(request.newEmail());
			memberEmailVerificationService.clearVerification(request.newEmail());
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("Failed to update email. Email already in use.", e);
		}
	}

	@Transactional
	public MemberResponse updateUsername(
		UpdateMemberUsernameRequest request,
		Long memberId
	) {
		Member member = memberReader.findMemberById(memberId);

		memberValidator.validateUsernameIsUnique(request.newUsername());

		try {
			member.updateUsername(request.newUsername());
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("Failed to update username. Username already in use.", e);
		}
	}

	@Transactional
	public MemberResponse updatePassword(
		UpdateMemberPasswordRequest request,
		Long memberId
	) {
		Member member = memberReader.findMemberById(memberId);

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getLoginId(), request.originalPassword())
		);

		// memberValidator.validateMemberPassword(request.originalPassword(), memberId);

		member.updatePassword(passwordEncoder.encode(request.newPassword()));

		return MemberResponse.from(member);
	}

	/**
	 * Todo
	 *  - hard delete X
	 *  - INACTIVE 또는 WITHDRAW_REQUESTED 상태로 변경(MembershipStatus 만들기)
	 *  - 추후에 스케쥴을 사용해서 배치로 삭제
	 *  - INACTIVE 상태인 멤버는 로그인 불가능하도록 막기(기존 로그인 세션도 전부 제거)
	 */
	@Transactional
	public void withdraw(
		WithdrawMemberRequest request,
		Long memberId
	) {
		Member member = memberReader.findMemberById(memberId);

		// memberValidator.validateMemberPassword(request.password(), memberId);

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getLoginId(), request.password())
		);

		memberValidator.validateMemberHasNoOwnedWorkspaces(memberId);

		memberRepository.delete(member);
	}

	private void updateMemberInfoIfPresent(
		UpdateMemberProfileRequest request,
		Member member
	) {
		if (request.hasName()) {
			member.updateName(request.name());
		}
		if (request.hasBirthDate()) {
			member.updateBirthDate(request.birthDate());
		}
		if (request.hasJobType()) {
			member.updateJobType(request.jobType());
		}
	}
}
