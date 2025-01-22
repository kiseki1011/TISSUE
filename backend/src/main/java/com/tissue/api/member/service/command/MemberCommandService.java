package com.tissue.api.member.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberNameRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberEmailResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberInfoResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberNameResponse;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.security.PasswordEncoder;

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
		memberValidator.validateLoginIdIsUnique(request.loginId());
		memberValidator.validateEmailIsUnique(request.email());

		String encodedPassword = passwordEncoder.encode(request.password());
		Member member = request.toEntity(encodedPassword);

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

	@Transactional
	public UpdateMemberNameResponse updateName(
		UpdateMemberNameRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		member.updateName(Name.builder()
			.firstName(request.firstName())
			.lastName(request.lastName())
			.build());

		return UpdateMemberNameResponse.from(member);
	}

	@Transactional
	public UpdateMemberEmailResponse updateEmail(
		UpdateMemberEmailRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		String newEmail = request.newEmail();
		memberValidator.validateEmailIsUnique(newEmail);

		member.updateEmail(newEmail);

		return UpdateMemberEmailResponse.from(member);
	}

	@Transactional
	public void updatePassword(
		UpdateMemberPasswordRequest request,
		Long memberId
	) {
		Member member = findMemberById(memberId);

		String encodedNewPassword = passwordEncoder.encode(request.newPassword());

		member.updatePassword(encodedNewPassword);
	}

	@Transactional
	public void withdraw(
		Long memberId
	) {
		Member member = findMemberById(memberId);

		memberValidator.validateMemberHasNoOwnedWorkspaces(memberId);

		memberRepository.delete(member);
	}

	/**
	 * Todo
	 *  - MemberProfile이라는 VO를 만들어서 사용 고려
	 *  - or 요청 DTO에 검증 로직을 포함시키거나
	 */
	private void updateMemberInfoIfPresent(
		UpdateMemberInfoRequest request,
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

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));
	}
}
