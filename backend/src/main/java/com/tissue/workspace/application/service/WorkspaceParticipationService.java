package com.tissue.workspace.application.service;

import static com.tissue.member.domain.MemberStatus.ACTIVE;

import com.tissue.common.enums.JoinMethod;
import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.policy.MemberPolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.InviteToProjectCommand;
import com.tissue.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.workspace.application.port.out.InvitationCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.InvitationFinder;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.policy.WorkspacePolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceParticipationService implements WorkspaceParticipationUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final InvitationFinder invitationFinder;
    private final MemberQueryRepository memberQueryRepository;
    private final InvitationCommandRepository invitationRepository;
    private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final WorkspacePolicy workspacePolicy;
    private final MemberPolicy memberPolicy;
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final ProjectAuthorizationService projectAuthService;
    private final WorkspaceEventPublisher eventPublisher;

    // TODO: inviteToWorkspace, inviteToProject는 따로 WorkspaceInvitationService로 분리할까?
    @Override
    public InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        return processInvitation(workspace, cmd.emails(), cmd.role(), cmd.targetProjects());
    }

    @Override
    public InviteMembersResponse inviteToProject(InviteToProjectCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        projectAuthService.requireProjectAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        List<ProjectJoinConfigDto> singleProjectConfig =
                List.of(new ProjectJoinConfigDto(actorContext.projectKey(), cmd.role()));

        return processInvitation(workspace, cmd.emails(), WorkspaceRole.MEMBER, singleProjectConfig);
    }

    @Override
    public void leave(WorkspaceMemberContext actorContext) {
        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        WorkspaceMember actor = workspaceMemberFinder.getActive(actorContext.memberId(), workspace);

        workspacePolicy.ensureCanLeaveWorkspace(actor);

        actor.softDelete();

        // TODO: workspaceId + memberId로 동작하도록 변경할까?
        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(
                actorContext.workspaceKey(), actorContext.memberId());

        // TODO: WorkspaceMemberLeftEvent
    }

    @Override
    public void kick(KickWorkspaceMemberCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        WorkspaceMember target = workspaceMemberFinder.getActive(cmd.targetMemberId(), workspace);

        target.softDelete();

        // TODO: workspaceId + memberId로 동작하도록 변경할까?
        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(
                actorContext.workspaceKey(), cmd.targetMemberId());

        // TODO: WorkspaceMemberKickedEvent
    }

    // TODO: add a javadoc for the next information
    //  - this method is not a implementation of a UseCase
    //  - this method is called from other services (a method for internal use)
    //  - controller does not know this method unless it directly depends on this service
    // TODO: actorMemberId를 파라미터로 받자(이벤트 컨텍스트로 넘기기 위해서, @Nullable 붙이고)
    protected WorkspaceMember join(
            Workspace workspace,
            Member member,
            WorkspaceRole role,
            Long actorMemberId,
            @Nullable String actorDisplayName,
            JoinMethod joinMethod) {

        Optional<WorkspaceMember> activeMember = workspaceMemberFinder.getActiveOptionalBy(member, workspace);
        if (activeMember.isPresent()) {
            return activeMember.get();
        }

        checkWorkspaceCapacity(workspace);
        checkMemberJoinCapacity(member);

        WorkspaceMember joinedMember = workspaceMemberFinder
                .getOptionalBy(member.getId(), workspace.getKey())
                .map(returningMember -> {
                    returningMember.restoreSoftDeleted();
                    return returningMember;
                })
                .orElseGet(() -> {
                    WorkspaceMember newMember = WorkspaceMember.create(member, workspace, role);
                    return workspaceMemberCommandRepository.save(newMember);
                });

        eventPublisher.publishMemberJoinedWorkspace(joinedMember, joinMethod, actorMemberId, actorDisplayName);

        return joinedMember;
    }

    private InviteMembersResponse processInvitation(
            Workspace workspace,
            Set<String> emails,
            WorkspaceRole roleToGrant,
            Collection<ProjectJoinConfigDto> projectConfigs) {
        InvitationFilterResult filterResult = filterInvitableMembers(workspace.getKey(), emails);

        List<Member> targetMembers = filterResult.targets();
        List<Member> skippedMembers = filterResult.skipped();

        for (Member member : targetMembers) {
            Invitation invitation = Invitation.create(workspace, member, roleToGrant);

            if (projectConfigs != null) {
                for (var config : projectConfigs) {
                    Project project = projectFinder.getModifiableBy(config.projectKey(), workspace.getKey());
                    invitation.addProjectConfig(project, config.role());
                }
            }
            invitationRepository.save(invitation);
        }

        // TODO: InvitationSentEvent - targetMembers에게만 발송

        return InviteMembersResponse.from(workspace.getKey(), targetMembers, skippedMembers);
    }

    private InvitationFilterResult filterInvitableMembers(String workspaceKey, Set<String> emails) {
        List<Member> candidates = memberQueryRepository.findAllByEmailInAndStatus(emails, ACTIVE);
        if (candidates.isEmpty()) {
            return new InvitationFilterResult(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> candidateIds = candidates.stream().map(Member::getId).toList();

        Set<Long> joinedIds = workspaceMemberFinder.getJoinedMemberIdsBy(workspaceKey, candidateIds);
        Set<Long> pendingIds = invitationFinder.findPendingMemberIds(workspaceKey, candidateIds);

        Map<Boolean, List<Member>> partitioned = candidates.stream()
                .collect(Collectors.partitioningBy(
                        m -> !joinedIds.contains(m.getId()) && !pendingIds.contains(m.getId())));

        List<Member> targets = partitioned.getOrDefault(true, Collections.emptyList());
        List<Member> skipped = partitioned.getOrDefault(false, Collections.emptyList());

        return new InvitationFilterResult(targets, skipped);
    }

    private void checkWorkspaceCapacity(Workspace workspace) {
        int currentCount = workspaceMemberFinder.countTotalMembersBy(workspace.getKey());
        workspacePolicy.ensureCanAddMember(workspace.getKey(), currentCount);
    }

    private void checkMemberJoinCapacity(Member member) {
        int joinedCount = workspaceMemberFinder.countJoinedWorkspacesBy(member);
        memberPolicy.ensureCanJoinWorkspace(joinedCount, member);
    }

    private record InvitationFilterResult(List<Member> targets, List<Member> skipped) {}
}
