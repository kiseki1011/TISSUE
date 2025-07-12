package com.tissue.api.member.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.infrastructure.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberReader {

	private final MemberRepository memberRepository;

	public Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));
	}

	public Member findMemberByLoginIdOrEmail(String identifier) {
		return memberRepository.findByLoginIdOrEmail(identifier)
			.orElseThrow(() -> new MemberNotFoundException(identifier));
	}
}
