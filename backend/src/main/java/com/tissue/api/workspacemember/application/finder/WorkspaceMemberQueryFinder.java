package com.tissue.api.workspacemember.application.finder;

import org.springframework.stereotype.Service;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberQueryFinder {

	private final WorkspaceMemberQueryRepository queryRepo;

	public WorkspaceMember findIncludingArchived(Long memberId, String workspaceKey) {
		return queryRepo.findIncludingArchived(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}
}
