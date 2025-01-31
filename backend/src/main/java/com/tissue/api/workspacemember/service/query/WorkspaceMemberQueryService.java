package com.tissue.api.workspacemember.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberQueryService {
	/*
	 * Todo
	 *  - getWorkspaceMembers: 특정 워크스페이스에 참여하는 모든 멤버 조회
	 */
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public WorkspaceMember findWorkspaceMember(Long id, String code) {
		return workspaceMemberRepository.findByIdAndWorkspaceCode(id, code)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id, code));
	}
}
