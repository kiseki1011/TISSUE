package com.tissue.api.member.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.presentation.dto.response.MyProfileResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MemberQueryService {

	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public MyProfileResponse getMyProfile(Long memberId) {
		Member member = findMember(memberId);
		return MyProfileResponse.from(member);
	}

	@Transactional(readOnly = true)
	public Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));
	}

	@Transactional(readOnly = true)
	public Member findMember(String identifier) {
		return memberRepository.findByIdentifier(identifier)
			.orElseThrow(() -> new MemberNotFoundException(
				String.format("Member not found with login ID or email. identifier: %s", identifier)));
	}
}
