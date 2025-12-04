package com.tissue.api.workspace.application.service.command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.api.workspace.application.dto.request.InviteToProjectCommand;
import com.tissue.api.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.response.InviteMembersResponse;
import com.tissue.api.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.api.workspace.application.port.out.InvitationCommandRepository;
import com.tissue.api.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.api.workspace.application.service.finder.InvitationFinder;
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
	private final ProjectFinder projectFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final InvitationFinder invitationFinder;
	private final MemberRepository memberRepository;
	private final InvitationCommandRepository invitationRepository;
	private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	private final WorkspacePolicy workspacePolicy;

	// private final ApplicationEventPublisher eventPublisher;

	@Override
	public InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		return processInvitation(
			workspace,
			cmd.emails(),
			cmd.role(),
			cmd.targetProjects()
		);
	}

	@Override
	public InviteMembersResponse inviteToProject(InviteToProjectCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		List<ProjectJoinConfigDto> singleProjectConfig = List.of(
			new ProjectJoinConfigDto(cmd.projectKey(), cmd.role())
		);

		return processInvitation(
			workspace,
			cmd.emails(),
			WorkspaceRole.MEMBER,
			singleProjectConfig
		);
	}

	@Override
	public void leave(String workspaceKey, Long memberId) {
		Workspace workspace = workspaceFinder.findByKey(workspaceKey);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(memberId, workspace);

		workspacePolicy.ensureCanLeaveWorkspace(workspaceMember);
		workspaceMember.softDelete();

		// TODO: WorkspaceMemberLeftEvent
	}

	@Override
	public void kick(KickWorkspaceMemberCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());
		WorkspaceMember actor = workspaceMemberFinder.findBy(cmd.actorMemberId(), workspace);
		WorkspaceMember target = workspaceMemberFinder.findBy(cmd.targetMemberId(), workspace);

		target.softDelete();

		// TODO: WorkspaceMemberKickedEvent
	}

	// TODO: 주석에 UseCase에 포함되지 않고 다른 서비스에서 호출하는 용도로 구현되어 있다는 것을 설명
	//   - UseCase 인터페이스에는 없지만 다른 서비스가 쓰도록 열어둔 메서드 (내부용)
	//   - Controller는 이 메서드를 모름 (UseCase 인터페이스로 주입받으므로)
	//   - 다른 애플리케이션 서비스(UseCase 구현체)는 이 구현체 클래스를 주입받아 호출 가능
	@Transactional
	public WorkspaceMember join(Workspace workspace, Member member, WorkspaceRole role) {
		Optional<WorkspaceMember> activeMember = workspaceMemberFinder.findOptionalBy(member, workspace);
		if (activeMember.isPresent()) {
			return activeMember.get();
		}

		checkWorkspaceCapacity(workspace);

		return workspaceMemberFinder.findAnyOptionalBy(member.getId(), workspace.getKey())
			.map(returningMember -> {
				returningMember.restore();
				return returningMember;
			})
			.orElseGet(() -> {
				WorkspaceMember newMember = WorkspaceMember.create(member, workspace, role);
				return workspaceMemberCommandRepository.save(newMember);
			});
	}

	private InviteMembersResponse processInvitation(
		Workspace workspace,
		Set<String> emails,
		WorkspaceRole roleToGrant,
		Collection<ProjectJoinConfigDto> projectConfigs
	) {
		InvitationFilterResult filterResult = filterInvitableMembers(workspace.getKey(), emails);

		List<Member> targetMembers = filterResult.targets();
		List<Member> skippedMembers = filterResult.skipped();

		for (Member member : targetMembers) {
			Invitation invitation = Invitation.create(workspace, member, roleToGrant);

			if (projectConfigs != null) {
				for (var config : projectConfigs) {
					Project project = projectFinder.findBy(config.projectKey(), workspace.getKey());
					invitation.addProjectConfig(project, config.role());
				}
			}
			invitationRepository.save(invitation);
		}

		// TODO: InvitationSentEvent - targetMembers에게만 발송

		return InviteMembersResponse.from(
			workspace.getKey(),
			targetMembers,
			skippedMembers
		);
	}

	private InvitationFilterResult filterInvitableMembers(String workspaceKey, Set<String> emails) {
		List<Member> candidates = memberRepository.findAllByEmailIn(emails);
		if (candidates.isEmpty()) {
			return new InvitationFilterResult(Collections.emptyList(), Collections.emptyList());
		}

		List<Long> candidateIds = candidates.stream()
			.map(Member::getId)
			.toList();

		Set<Long> joinedIds = workspaceMemberFinder.findJoinedMemberIdsBy(workspaceKey, candidateIds);
		Set<Long> pendingIds = invitationFinder.findPendingMemberIds(workspaceKey, candidateIds);

		Map<Boolean, List<Member>> partitioned = candidates.stream()
			.collect(Collectors.partitioningBy(m ->
				!joinedIds.contains(m.getId()) && !pendingIds.contains(m.getId())
			));

		return new InvitationFilterResult(partitioned.get(true), partitioned.get(false));
	}

	private void checkWorkspaceCapacity(Workspace workspace) {
		int currentCount = workspaceMemberFinder.countTotalMembersBy(workspace.getKey());
		workspacePolicy.ensureCanAddMember(workspace.getKey(), currentCount);
	}

	private record InvitationFilterResult(
		List<Member> targets,
		List<Member> skipped
	) {
	}
}
