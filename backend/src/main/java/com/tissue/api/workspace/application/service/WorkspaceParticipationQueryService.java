package com.tissue.api.workspace.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.adapter.in.web.dto.WorkspaceDetail;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberRepository;
import com.tissue.api.workspace.adapter.in.web.dto.response.GetWorkspacesResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationQueryService {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public GetWorkspacesResponse getWorkspaces(Long memberId, Pageable pageable) {
		Page<WorkspaceDetail> workspaceDetails = workspaceMemberRepository.findByMember_Id(memberId, pageable)
			.map(workspaceMember -> WorkspaceDetail.from(
				workspaceMember.getWorkspace()
			));

		return GetWorkspacesResponse.from(workspaceDetails.getContent(), workspaceDetails.getTotalElements());
	}
}
