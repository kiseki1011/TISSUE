package com.tissue.api.workspacemember.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	public WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id));
	}

	public WorkspaceMember findWorkspaceMember(Long memberId, String workspaceKey) {
		return workspaceMemberRepository.findByMember_IdAndWorkspace_Key(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));
	}
}
