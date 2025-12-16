package com.tissue.member.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberNotFoundException;
import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.application.dto.response.GetProfileResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberQueryService {

	private final MemberQueryRepository memberQueryRepository;

	/**
	 * Todo
	 *  - DTO(ProfileResponse)로 응답을 받도록 MemberQueryRepository에 메서드 정의, 수정
	 */
	@Transactional(readOnly = true)
	public GetProfileResponse getProfile(Long memberId) {
		Member member = memberQueryRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));

		return GetProfileResponse.from(member);
	}
}
