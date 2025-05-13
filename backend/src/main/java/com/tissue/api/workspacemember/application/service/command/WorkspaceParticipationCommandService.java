package com.tissue.api.workspacemember.application.service.command;

import static com.tissue.api.workspacemember.domain.WorkspaceMember.*;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.application.service.command.MemberReader;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.domain.service.WorkspaceMemberPermissionValidator;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationCommandService {
	/*
	 * Todo
	 *  - leaveWorkspace: 워크스페이스 떠나기(현재 OWNER 상태면 불가능)
	 */
	private final WorkspaceReader workspaceReader;
	private final MemberReader memberReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberPermissionValidator workspaceMemberPermissionValidator;

	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 참여할 워크스페이스의 코드를 통해
	 * 참여를 요청한 로그인 멤버를 해당 워크스페이스에 참여시킨다.
	 *
	 * @param workspaceCode     - 워크스페이스의 고유 코드
	 * @param memberId - 세션에서 꺼낸 멤버 id(PK)
	 * @return JoinWorkspaceResponse - 워크스페이스 참여 응답을 위한 DTO
	 */
	@Transactional
	public WorkspaceMemberResponse joinWorkspace(
		String workspaceCode,
		Long memberId
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);
		Member member = memberReader.findMember(memberId);

		if (workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, workspaceCode)) {
			throw new InvalidOperationException(String.format(
				"Member already joined this workspace. memberId: %d, workspaceCode: %s",
				memberId, workspaceCode)
			);
		}

		WorkspaceMember workspaceMember = addWorkspaceMember(member, workspace);
		workspaceMemberRepository.save(workspaceMember);

		eventPublisher.publishEvent(
			MemberJoinedWorkspaceEvent.createEvent(workspaceMember)
		);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public void leaveWorkspace(
		String workspaceCode,
		Long memberId
	) {
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		workspaceMember.validateCanLeaveWorkspace();

		workspaceMember.remove();
	}
}
