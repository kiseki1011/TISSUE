package com.tissue.api.workspacemember.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.AlreadyJoinedWorkspaceException;
import com.tissue.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.tissue.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationCommandService {
	/*
	 * Todo
	 *  - leaveWorkspace: 워크스페이스 떠나기(현재 OWNER 상태면 불가능)
	 */
	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceValidator workspaceValidator;

	/**
	 * 참여할 워크스페이스의 코드와 참여 요청을 통해
	 * 참여를 요청한 로그인 멤버를 해당 워크스페이스에 참여시킨다.
	 *
	 * @param code     - 워크스페이스의 고유 코드
	 * @param request  - 워크스페이스 참여 요청 객체
	 * @param memberId - 세션에서 꺼낸 멤버 id(PK)
	 * @return - 워크스페이스 참여 응답을 위한 DTO
	 */
	@Transactional
	public JoinWorkspaceResponse joinWorkspace(String code, JoinWorkspaceRequest request, Long memberId) {

		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		if (workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, code)) {
			throw new AlreadyJoinedWorkspaceException();
		}

		workspaceValidator.validatePasswordIfExists(workspace.getPassword(), request.getPassword());

		WorkspaceMember workspaceMember = WorkspaceMember.addCollaboratorWorkspaceMember(member, workspace);
		workspaceMemberRepository.save(workspaceMember);

		return JoinWorkspaceResponse.from(workspaceMember);
	}
}
