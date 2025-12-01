package com.tissue.api.workspace.application.service.command;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.workspace.application.dto.request.InviteMembersCommand;
import com.tissue.api.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.response.InviteMembersResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.api.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.Invitation;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationService implements WorkspaceParticipationUseCase {

	private final WorkspaceFinder workspaceFinder;
	private final MemberFinder memberFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	private final MemberRepository memberRepository;
	private final InvitationRepository invitationRepository;
	private final WorkspacePolicy workspacePolicy;
	// private final MemberPolicy memberPolicy;
	// private final ApplicationEventPublisher eventPublisher;

	public InviteMembersResult inviteToWorkspace(InviteMembersCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		List<Member> members = filterInvitableMembers(cmd.workspaceKey(), cmd.emails());

		members.forEach(member -> createInvitation(workspace, member));

		// TODO: MembersInvitedEvent vs WorkspaceMembersInvitedEvent

		return InviteMembersResult.from(cmd.workspaceKey(), members);
	}

	public WorkspaceMemberCommandResult join(String workspaceKey, Long memberId) {
		Workspace workspace = workspaceFinder.findByKey(workspaceKey);
		Member member = memberFinder.findMemberById(memberId);

		Optional<WorkspaceMember> existingMemberOpt = workspaceMemberCommandRepository
			.findByMemberAndWorkspace(member, workspace);

		if (existingMemberOpt.isPresent()) {
			return WorkspaceMemberCommandResult.from(existingMemberOpt.get());
		}

		// TODO: ensureCanJoin은 삭제할까? 아니면 일정 수준을 넘지 못하도록 제한을 둘까? 악의적인 참여 사용을 못하도록
		//  500개 선에서 막는게 좋을 것 같긴함
		// memberPolicy.ensureCanJoin(member);
		// workspacePolicy.ensureCanAddMember(workspace);

		// TODO: 서비스의 파라미터로 WorkspaceRole도 받아서 role을 넘기기
		WorkspaceMember workspaceMember = workspace.addMember(member, WorkspaceRole.MEMBER);

		// TODO: MemberJoinedWorkspaceEvent vs WorkspaceMemberJoinedEvent

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceCommandResult leave(String workspaceKey, Long memberId) {
		Workspace workspace = workspaceFinder.findByKey(workspaceKey);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspace(memberId, workspace);

		// TODO: workspaceMemberValidator 또는 workspacePolicy 또는 workspaceMemberPolicy 만들어서 사용
		//  예시: workspacePolicy.ensureCanLeaveWorkspace(workspaceMember);
		workspaceMember.validateCanLeaveWorkspace();

		workspace.removeMember(workspaceMember);

		// TODO: WorkspaceMemberLeftEvent

		return WorkspaceCommandResult.from(workspace);
	}

	public WorkspaceMemberCommandResult kick(KickWorkspaceMemberCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());
		WorkspaceMember actor = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.actorMemberId(), workspace);
		WorkspaceMember target = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.targetMemberId(), workspace);

		workspace.removeMember(target);

		// TODO: WorkspaceMemberKickedEvent

		return WorkspaceMemberCommandResult.from(target);
	}

	private List<Member> filterInvitableMembers(String workspaceKey, Set<String> emails) {
		Set<Long> existingMemberIds = invitationRepository.findExistingMemberIds(workspaceKey);

		return memberRepository.findAllByEmailIn(emails).stream()
			.filter(member -> !existingMemberIds.contains(member.getId()))
			.toList();
	}

	private void createInvitation(Workspace workspace, Member member) {
		Invitation invitation = Invitation.createPendingInvitation(workspace, member);
		invitationRepository.save(invitation);
	}
}
