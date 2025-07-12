package com.tissue.api.workspacemember.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberReader {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id));
	}

	// TODO: introduce caching
	@Transactional(readOnly = true)
	public WorkspaceMember findWorkspaceMember(Long memberId, String code) {
		return workspaceMemberRepository.findByMemberIdAndWorkspaceCode(memberId, code)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, code));
	}
}
