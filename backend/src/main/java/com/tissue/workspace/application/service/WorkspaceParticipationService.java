package com.tissue.workspace.application.service;

import static com.tissue.member.domain.MemberStatus.ACTIVE;

import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.policy.MemberPolicy;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.service.finder.ProjectFinder;
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

    @Override
    public InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        return processInvitation(workspace, cmd.emails(), cmd.role(), cmd.targetProjectKeys());
    }

    @Override
    public InviteMembersResponse inviteToProject(InviteToProjectCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        // TODO: requireWorspaceAdmin or requireProjectCreator -> requireProjectEditPermission

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        List<String> singleProjectKey = List.of(actorContext.projectKey());

        return processInvitation(workspace, cmd.emails(), WorkspaceRole.MEMBER, singleProjectKey);
    }

    @Override
    public void leave(WorkspaceMemberContext actorContext) {
        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());
        WorkspaceMember actor = workspaceMemberFinder.getBy(workspace, actorContext.memberId());

        workspacePolicy.ensureCanLeaveWorkspace(actor);

        actor.softDelete();

        // TODO: projectCommandRepository.deleteAllByWorkspaceKeyAndMemberId (or deleteAllByWorkspaceMember)
        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(
                actorContext.workspaceKey(), actorContext.memberId());

        // TODO: WorkspaceMemberLeftEvent
    }

    @Override
    public void kick(KickWorkspaceMemberCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());
        WorkspaceMember target = workspaceMemberFinder.getBy(workspace, cmd.targetMemberId());

        target.softDelete();

        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(
                actorContext.workspaceKey(), cmd.targetMemberId());

        // TODO: WorkspaceMemberKickedEvent
    }

    // TODO: add a javadoc for the next information
    //  - this method is not a implementation of a UseCase
    //  - this method is called from other services (a method for internal use)
    //  - controller does not know this method unless it directly depends on this service
    protected WorkspaceMember join(Workspace workspace, Member member, WorkspaceRole role) {

        // TODO: Needs refactoring after WorkspaceMember, ProjectMember delete policy change
        Optional<WorkspaceMember> activeMember = workspaceMemberFinder.getOptionalBy(workspace, member);
        if (activeMember.isPresent()) {
            return activeMember.get();
        }

        checkWorkspaceCapacity(workspace);
        checkMemberJoinCapacity(member);

        WorkspaceMember joinedMember = workspaceMemberFinder
                .getOptionalBy(workspace, member)
                .map(returningMember -> {
                    returningMember.restoreSoftDeleted();
                    return returningMember;
                })
                .orElseGet(() -> {
                    WorkspaceMember newMember = WorkspaceMember.create(member, workspace, role);
                    return workspaceMemberCommandRepository.save(newMember);
                });

        return joinedMember;
    }

    private InviteMembersResponse processInvitation(
            Workspace workspace, Set<String> emails, WorkspaceRole roleToGrant, Collection<String> projectKeys) {

        InvitationFilterResult filterResult = filterInvitableMembers(workspace.getKey(), emails);
        List<Member> targetMembers = filterResult.targets();
        List<Member> skippedMembers = filterResult.skipped();

        for (Member member : targetMembers) {
            Invitation invitation = Invitation.create(workspace, member, roleToGrant);

            if (projectKeys != null) {
                for (var projectKey : projectKeys) {
                    projectFinder.getWithWorkspaceBy(workspace.getKey(), projectKey);
                    invitation.addProjectKey(projectKey);
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
