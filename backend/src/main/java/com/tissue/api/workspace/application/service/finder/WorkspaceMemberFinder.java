package com.tissue.api.workspace.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberQueryRepository;

import lombok.RequiredArgsConstructor;

// TODO: WorkspaceMember soft-delete 관련 리팩토링 진행 후 개선
@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

	private final WorkspaceMemberQueryRepository queryRepo;

	public WorkspaceMember findByMemberIdAndWorkspaceKey(Long memberId, String workspaceKey) {
		return queryRepo.findByMember_IdAndWorkspace_Key(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}

	public WorkspaceMember findByMemberIdAndWorkspace(Long memberId, Workspace workspace) {
		return queryRepo.findByMember_IdAndWorkspace(memberId, workspace)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspace.getKey()));
	}

	public WorkspaceMember findIncludingArchived(Long memberId, String workspaceKey) {
		return queryRepo.findIncludingArchived(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}

	public List<WorkspaceMember> findAllBy(Collection<Long> memberIds, String workspaceKey) {
		return queryRepo.findAllByMember_IdInAndWorkspaceKey(memberIds, workspaceKey);
	}
}
