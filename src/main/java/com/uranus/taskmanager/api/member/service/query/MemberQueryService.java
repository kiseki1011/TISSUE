package com.uranus.taskmanager.api.member.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateAuthRequest;
import com.uranus.taskmanager.api.member.validator.MemberValidator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MemberQueryService {

	private final MemberRepository memberRepository;
	private final MemberValidator memberValidator;

	@Transactional(readOnly = true)
	public void validatePasswordForUpdate(UpdateAuthRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		memberValidator.validatePassword(request.getPassword(), member.getPassword());
	}

	// @Transactional(readOnly = true)
	// public MyWorkspacesResponse getMyWorkspaces(Long memberId, Pageable pageable) {
	// 	Page<WorkspaceDetail> workspaceDetails = workspaceMemberRepository.findByMemberId(memberId, pageable)
	// 		.map(workspaceMember -> WorkspaceDetail.from(
	// 			workspaceMember.getWorkspace()
	// 		));
	//
	// 	return MyWorkspacesResponse.from(workspaceDetails.getContent(), workspaceDetails.getTotalElements());
	// }
}
