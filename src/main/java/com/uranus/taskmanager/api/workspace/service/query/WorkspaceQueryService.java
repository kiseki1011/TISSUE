package com.uranus.taskmanager.api.workspace.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceQueryService {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getWorkspaceDetail(String workspaceCode) {

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		return WorkspaceDetail.from(workspace);
	}

	// @Transactional(readOnly = true)
	// public Long getWorkspaceMemberId(String code, Long id) {
	//
	// 	WorkspaceMember workspaceMember = workspaceMemberRepository
	// 		.findByMemberIdAndWorkspaceCode(id, code)
	// 		.orElseThrow(MemberNotInWorkspaceException::new);
	//
	// 	return workspaceMember.getId();
	// }
}
