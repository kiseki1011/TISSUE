package com.tissue.api.member.application.service.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberProfileRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberUsernameRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

	private final MemberReader memberReader;
	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public MemberResponse signup(
		SignupMemberRequest request
	) {
		memberValidator.validateLoginIdIsUnique(request.loginId());
		memberValidator.validateEmailIsUnique(request.email());
		memberValidator.validateUsernameIsUnique(request.username());

		String encodedPassword = passwordEncoder.encode(request.password());
		Member member = request.toEntity(encodedPassword);

		try {
			Member savedMember = memberRepository.save(member);
			return MemberResponse.from(savedMember);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("회원가입에 실패했습니다.", e);
		}
	}

	@Transactional
	public MemberResponse updateInfo(
		UpdateMemberProfileRequest request,
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);

		updateMemberInfoIfPresent(request, member);

		return MemberResponse.from(member);
	}

	@Transactional
	public MemberResponse updateEmail(
		UpdateMemberEmailRequest request,
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);

		String newEmail = request.newEmail();
		memberValidator.validateEmailIsUnique(newEmail);

		try {
			member.updateEmail(newEmail);
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("중복된 Email입니다", e);
		}
	}

	@Transactional
	public MemberResponse updateUsername(
		UpdateMemberUsernameRequest request,
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);

		memberValidator.validateUsernameIsUnique(request.newUsername());

		try {
			member.updateUsername(request.newUsername());
			return MemberResponse.from(member);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateResourceException("중복된 username입니다", e);
		}
	}

	@Transactional
	public MemberResponse updatePassword(
		UpdateMemberPasswordRequest request,
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);
		memberValidator.validateMemberPassword(request.originalPassword(), memberId);

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
		Member member = memberReader.findMember(memberId);
		memberValidator.validateMemberPassword(request.password(), memberId);
		memberValidator.validateMemberHasNoOwnedWorkspaces(memberId);

		memberRepository.delete(member);
	}

	private void updateMemberInfoIfPresent(
		UpdateMemberProfileRequest request,
		Member member
	) {
		if (request.hasName()) {
			member.updateName(Name.builder()
				.firstName(request.firstName())
				.lastName(request.lastName())
				.build());
		}
		if (request.hasBirthDate()) {
			member.updateBirthDate(request.birthDate());
		}
		if (request.hasJobType()) {
			member.updateJobType(request.jobType());
		}
		if (request.hasBiography()) {
			member.updateBiography(request.biography());
		}
	}
}
