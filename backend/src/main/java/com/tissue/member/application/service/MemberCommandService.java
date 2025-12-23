package com.tissue.member.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.out.MemberRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

	private final MemberFinder memberFinder;
	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final MemberEmailVerificationService memberEmailVerificationService;

	@Override
	@Transactional
	public MemberSignupResponse signup(SignupMemberCommand cmd) {
		memberValidator.ensureUniqueEmail(cmd.email());
		memberValidator.ensureUniqueUsername(cmd.username());

		memberEmailVerificationService.validateEmailVerified(cmd.email());

		Member member = Member.create(
			cmd.email(),
			cmd.username(),
			passwordEncoder.encode(cmd.password()),
			cmd.name()
		);

		try {
			Member savedMember = memberRepository.save(member);
			memberEmailVerificationService.clearVerification(cmd.email());
			return MemberSignupResponse.from(savedMember);
		} catch (DataIntegrityViolationException e) {
			throw MemberExceptions.signUpConflict(cmd.email(), cmd.username(), e);
		}
	}

	@Override
	@Transactional
	public void updateName(String name, Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);
		member.updateName(name);
	}

	@Override
	@Transactional
	public void updateEmail(String newEmail, Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);

		memberValidator.ensureUniqueEmail(newEmail);
		memberEmailVerificationService.validateEmailVerified(newEmail);

		try {
			member.updateEmail(newEmail);
			memberEmailVerificationService.clearVerification(newEmail);
		} catch (DataIntegrityViolationException e) {
			throw MemberExceptions.duplicateEmail(newEmail, e);
		}
	}

	@Override
	@Transactional
	public void updateUsername(String newUsername, Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);

		memberValidator.ensureUniqueUsername(newUsername);

		try {
			member.updateUsername(newUsername);
		} catch (DataIntegrityViolationException e) {
			throw MemberExceptions.duplicateUsername(newUsername, e);
		}
	}

	@Override
	@Transactional
	public void updatePassword(String originalPassword, String newPassword, Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);

		// TODO: is there a better way to do this?
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword)
		);

		member.updatePassword(passwordEncoder.encode(newPassword));
	}

	// TODO(later): will implement a scheduler that batch (hard) deletes Members with DELETED status
	//  - set policy to store Member for 30 days(configurable), then hard-delete
	// TODO(now): but how should i handle the WorkspaceMembers and ProjectMembers of this member? soft-delete(kick) them all?
	@Override
	@Transactional
	public void withdraw(String password, Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);

		// TODO: is there a better way to do this?
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(member.getEmail(), password)
		);

		memberValidator.ensureWithdrawable(member);

		member.withdraw();
	}
}
