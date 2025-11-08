package com.tissue.api.workspacemember.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberQueryRepository;

import lombok.RequiredArgsConstructor;

// TODO: WorkspaceMember soft-delete 관련 리팩토링 진행 후 개선
@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

	private final WorkspaceMemberQueryRepository queryRepo;

	public WorkspaceMember findWorkspaceMember(Long memberId, String workspaceKey) {
		return queryRepo.findByMember_IdAndWorkspace_Key(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}

	public WorkspaceMember findIncludingArchived(Long memberId, String workspaceKey) {
		return queryRepo.findIncludingArchived(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}
}
