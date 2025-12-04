package com.tissue.api.member.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.exception.MemberNotFoundException;
import com.tissue.api.member.application.port.out.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFinder {

	private final MemberRepository memberRepository;

	public Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));
	}

	public Optional<Member> findOptionalBy(Long memberId) {
		return memberRepository.findById(memberId);
	}
}
