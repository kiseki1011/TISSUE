package com.uranus.taskmanager.api.member.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateAuthRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.member.validator.MemberValidator;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MemberQueryService {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	private final MemberValidator memberValidator;

	@Transactional(readOnly = true)
	public void validatePasswordForUpdate(UpdateAuthRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		memberValidator.validatePassword(request.getPassword(), member.getPassword());
	}

	@Transactional(readOnly = true)
	public MyWorkspacesResponse getMyWorkspaces(Long memberId, Pageable pageable) {
		Page<WorkspaceDetail> workspaceDetails = workspaceMemberRepository.findByMemberId(memberId, pageable)
			.map(workspaceMember -> WorkspaceDetail.from(
				workspaceMember.getWorkspace(),
				workspaceMember.getRole()
			));

		return MyWorkspacesResponse.from(workspaceDetails.getContent(), workspaceDetails.getTotalElements());
	}
}
