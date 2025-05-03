package com.tissue.api.member.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
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

	/**
	 * Todo
	 *  - 회원 가입도 동시성 관련 처리 필요
	 */
	@Transactional
	public MemberResponse signup(
		SignupMemberRequest request
	) {
		memberValidator.validateLoginIdIsUnique(request.loginId());
		memberValidator.validateEmailIsUnique(request.email());

		String encodedPassword = passwordEncoder.encode(request.password());
		Member member = request.toEntity(encodedPassword);

		Member savedMember = memberRepository.save(member);

		return MemberResponse.from(savedMember);
	}

	@Transactional
	public MemberResponse updateInfo(
		UpdateMemberInfoRequest request,
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

		member.updateEmail(newEmail);

		return MemberResponse.from(member);
	}

	@Transactional
	public MemberResponse updatePassword(
		UpdateMemberPasswordRequest request,
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);

		String encodedNewPassword = passwordEncoder.encode(request.newPassword());

		member.updatePassword(encodedNewPassword);

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
		Long memberId
	) {
		Member member = memberReader.findMember(memberId);

		memberValidator.validateMemberHasNoOwnedWorkspaces(memberId);

		memberRepository.delete(member);
	}

	// TODO: 이 방식을 개선
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
}
