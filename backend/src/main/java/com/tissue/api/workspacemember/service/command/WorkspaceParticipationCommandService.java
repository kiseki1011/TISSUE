package com.tissue.api.workspacemember.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.DuplicateResourceException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
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

	/**
	 * 참여할 워크스페이스의 코드를 통해
	 * 참여를 요청한 로그인 멤버를 해당 워크스페이스에 참여시킨다.
	 *
	 * @param workspaceCode     - 워크스페이스의 고유 코드
	 * @param memberId - 세션에서 꺼낸 멤버 id(PK)
	 * @return JoinWorkspaceResponse - 워크스페이스 참여 응답을 위한 DTO
	 */
	@Transactional
	public JoinWorkspaceResponse joinWorkspace(String workspaceCode, Long memberId) {

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));

		if (workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, workspaceCode)) {
			throw new DuplicateResourceException(
				String.format("Member already joined this workspace. memberId: %d, workspaceCode: %s",
					memberId, workspaceCode));
		}

		WorkspaceMember workspaceMember = WorkspaceMember.addCollaboratorWorkspaceMember(member, workspace);
		workspaceMemberRepository.save(workspaceMember);

		return JoinWorkspaceResponse.from(workspaceMember);
	}
}
