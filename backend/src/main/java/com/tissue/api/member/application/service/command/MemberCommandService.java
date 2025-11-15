package com.tissue.api.member.application.service.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.application.dto.SignupMemberCommand;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.exception.DuplicateEmailException;
import com.tissue.api.member.exception.DuplicateUsernameException;
import com.tissue.api.member.exception.MemberSignupConflictException;
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

	private final MemberFinder memberFinder;
	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	// TODO: MemberVerificationUseCase
	private final MemberEmailVerificationService memberEmailVerificationService;

	@Transactional
	public MemberResponse signup(SignupMemberCommand cmd) {
		memberValidator.ensureEmailIsUnique(cmd.email());
		memberValidator.ensureUsernameIsUnique(cmd.username());

		memberEmailVerificationService.validateEmailVerified(cmd.email());

		Member member = Member.create(
			cmd.email(),
			cmd.username(),
			passwordEncoder.encode(cmd.password()),
			cmd.name(),
			cmd.birthDate()
		);

		try {
			Member savedMember = memberRepository.save(member);
			memberEmailVerificationService.clearVerification(cmd.email());

			return MemberResponse.from(savedMember);

		} catch (DataIntegrityViolationException e) {
			throw new MemberSignupConflictException(cmd.email(), cmd.username(), e);
		}
	}

	// TODO: UpdateMemberProfileCommand
	@Transactional
	public MemberResponse updateProfile(UpdateMemberProfileRequest request, Long memberId) {
		Member member = memberFinder.findMemberById(memberId);

		// TODO: Patchers.apply를 사용하도록 리팩토링
		updateMemberInfoIfPresent(request, member);

		return MemberResponse.from(member);
	}

	// TODO: UpdateMemberEmailRequest -> String newEmail
	@Transactional
	public MemberResponse updateEmail(UpdateMemberEmailRequest request, Long memberId) {
		Member member = memberFinder.findMemberById(memberId);

		memberValidator.ensureEmailIsUnique(request.newEmail());
		memberEmailVerificationService.validateEmailVerified(request.newEmail());

		try {
			member.updateEmail(request.newEmail());
			memberEmailVerificationService.clearVerification(request.newEmail());
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateEmailException(request.newEmail(), e);
		}
	}

	// TODO: UpdateMemberUsernameRequest -> String newUsername
	@Transactional
	public MemberResponse updateUsername(UpdateMemberUsernameRequest request, Long memberId) {
		Member member = memberFinder.findMemberById(memberId);

		memberValidator.ensureUsernameIsUnique(request.newUsername());

		try {
			member.updateUsername(request.newUsername());
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateUsernameException(request.newUsername(), e);
		}
	}

	// TODO: UpdateMemberPasswordCommand
	@Transactional
	public MemberResponse updatePassword(UpdateMemberPasswordRequest request, Long memberId) {
		Member member = memberFinder.findMemberById(memberId);

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getEmail(), request.originalPassword())
		);

		member.updatePassword(passwordEncoder.encode(request.newPassword()));

		return MemberResponse.from(member);
	}

	/**
	 * Todo
	 *  - hard delete X
	 *  - 기존에 사용하던 soft-delete(base entity의 archived 필드) 방식 대신
	 *  INACTIVE 또는 WITHDRAW_REQUESTED 상태를 가진 MembershipStatus 만들어서 사용할까?
	 *  - 추후에 스케쥴을 사용해서 배치로 물리 삭제?
	 *  - INACTIVE 상태인 멤버는 로그인 불가능하도록 막기
	 */
	// TODO: WithdrawMemberRequest -> String password
	@Transactional
	public void withdraw(WithdrawMemberRequest request, Long memberId) {
		Member member = memberFinder.findMemberById(memberId);

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getEmail(), request.password())
		);

		memberValidator.ensureWithdrawable(member);

		memberRepository.delete(member);
	}

	// TODO: Patchers.apply를 사용하도록 리팩토링
	private void updateMemberInfoIfPresent(UpdateMemberProfileRequest request, Member member) {
		if (request.hasName()) {
			member.updateName(request.name());
		}
		if (request.hasBirthDate()) {
			member.updateBirthDate(request.birthDate());
		}
	}
}
