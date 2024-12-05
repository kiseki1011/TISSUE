package com.uranus.taskmanager.api.member.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdatePermissionRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MyProfileResponse;
import com.uranus.taskmanager.api.member.validator.MemberValidator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MemberQueryService {

	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;

	@Transactional(readOnly = true)
	public void validatePasswordForUpdate(UpdatePermissionRequest request, Long memberId) {
		Member member = findMemberById(memberId);
		memberValidator.validatePassword(request.getPassword(), member.getPassword());
	}

	@Transactional(readOnly = true)
	public MyProfileResponse getMyProfile(Long memberId) {
		Member member = findMemberById(memberId);
		return MyProfileResponse.from(member);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);
	}
}
