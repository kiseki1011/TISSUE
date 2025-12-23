package com.tissue.member.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.member.domain.exception.MemberExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFinder {

	private final MemberQueryRepository memberRepository;

	public Member getActiveBy(Long memberId) {
		return memberRepository.findByIdAndStatus(memberId, MemberStatus.ACTIVE)
			.orElseThrow(() -> MemberExceptions.activeNotFound(memberId));
	}

	public Optional<Member> findOptionalActiveBy(Long memberId) {
		return memberRepository.findByIdAndStatus(memberId, MemberStatus.ACTIVE);
	}
}
