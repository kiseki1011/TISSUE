package com.uranus.taskmanager.api.workspace.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceQueryService {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceRepository workspaceRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getWorkspaceDetail(String workspaceCode, Long memberId) {

		Workspace workspace = findWorkspaceByCode(workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(
			memberId, workspaceCode).orElseThrow(MemberNotInWorkspaceException::new);

		return WorkspaceDetail.from(workspace, workspaceMember.getRole());
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

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}
}
