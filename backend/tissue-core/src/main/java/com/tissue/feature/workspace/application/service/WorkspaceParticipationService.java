package com.tissue.feature.workspace.application.service;

import static com.tissue.feature.member.domain.MemberStatus.ACTIVE;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.policy.MemberPolicy;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceParticipationUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.InvitationFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
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
    public InviteMembersResponse inviteToWorkspace(String workspaceKey, InviteToWorkspaceCommand cmd, Long memberId) {

        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        return processInvitation(workspace, cmd.emails(), cmd.role(), cmd.targetProjectKeys());
    }

    @Override
    public void leave(String workspaceKey, Long memberId) {
        Workspace workspace = workspaceFinder.getBy(workspaceKey);
        WorkspaceMember actor = workspaceMemberFinder.getBy(workspace, memberId);

        workspacePolicy.ensureCanLeaveWorkspace(actor);

        actor.softDelete();

        // TODO: projectCommandRepository.deleteAllByWorkspaceKeyAndMemberId (or deleteAllByWorkspaceMember)
        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, memberId);

        // TODO: WorkspaceMemberLeftEvent
    }

    @Override
    public void kick(String workspaceKey, Long targetMemberId, Long memberId) {
        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);
        WorkspaceMember target = workspaceMemberFinder.getBy(workspace, targetMemberId);

        target.softDelete();

        projectMemberQueryRepository.softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, targetMemberId);

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
