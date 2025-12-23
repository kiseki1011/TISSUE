package com.tissue.member.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.member.application.dto.response.GetMemberProfile;
import com.tissue.member.application.port.in.MemberQueryUseCase;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.domain.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberQueryUseCase {

	private final MemberFinder memberFinder;

	@Override
	@Transactional(readOnly = true)
	public GetMemberProfile getMyProfile(Long memberId) {
		Member member = memberFinder.getActiveBy(memberId);

		return GetMemberProfile.from(member);
	}
}
