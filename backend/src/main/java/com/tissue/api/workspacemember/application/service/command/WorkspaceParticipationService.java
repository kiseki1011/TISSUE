package com.tissue.api.workspacemember.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationService {

	// Todo: Leave Workspace (cannot leave if OWNER)
	// TODO: Should the Workspace participation related APIs be in the WorkspaceMemberController?
	private final WorkspaceFinder workspaceFinder;
	private final MemberFinder memberFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspacePolicy workspacePolicy;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse joinWorkspace(
		String workspaceKey,
		Long memberId
	) {
		Workspace workspace = workspaceFinder.findWorkspaceWithMembers(workspaceKey);
		Member member = memberFinder.findMemberWithWorkspaces(memberId);

		// TODO: If im already join fetched WorkspaceMembers in the persistence context,
		//  can't i just check the List<WorkspaceMembers> to see if it exists without using workspaceMemberRepository?
		if (workspaceMemberRepository.existsByMember_IdAndWorkspace_Key(memberId, workspaceKey)) {
			throw new InvalidOperationException(String.format(
				"Member already joined this workspace. memberId: %d, workspaceKey: %s",
				memberId, workspaceKey)
			);
		}

		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			member,
			workspace,
			workspacePolicy
		);

		workspaceMemberRepository.save(workspaceMember);

		// eventPublisher.publishEvent(
		// 	MemberJoinedWorkspaceEvent.createEvent(workspaceMember)
		// );

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public void leaveWorkspace(
		String workspaceCode,
		Long memberId
	) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

		workspaceMember.validateCanLeaveWorkspace();

		workspaceMember.remove();
	}
}
