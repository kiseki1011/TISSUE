package com.uranus.taskmanager.api.workspacemember.service.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.MyWorkspacesResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberWorkspaceQueryService {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public MyWorkspacesResponse getMyWorkspaces(Long memberId, Pageable pageable) {
		Page<WorkspaceDetail> workspaceDetails = workspaceMemberRepository.findByMemberId(memberId, pageable)
			.map(workspaceMember -> WorkspaceDetail.from(
				workspaceMember.getWorkspace()
			));

		return MyWorkspacesResponse.from(workspaceDetails.getContent(), workspaceDetails.getTotalElements());
	}
}
