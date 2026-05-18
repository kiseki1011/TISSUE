package com.tissue.feature.workspace.application.service;

import static com.tissue.feature.member.domain.MemberStatus.ACTIVE;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
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
    private final ProjectMemberCommandRepository projectMemberCommandRepository;
    private final WorkspacePolicy workspacePolicy;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public InviteMembersResponse inviteToWorkspace(
            String workspaceKey, InviteToWorkspaceCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        return processInvitation(workspace, cmd.emails(), cmd.role(), cmd.targetProjectKeys());
    }

    @Override
    public void leave(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        workspacePolicy.ensureCanLeaveWorkspace(actor);

        actor.softDelete();

        projectMemberCommandRepository.softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, actorMemberId);
    }

    @Override
    public void kick(String workspaceKey, Long targetMemberId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        target.softDelete();

        projectMemberCommandRepository.softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, targetMemberId);
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

        return InviteMembersResponse.from(workspace.getKey(), targetMembers, skippedMembers);
    }

    private InvitationFilterResult filterInvitableMembers(String workspaceKey, Set<String> emails) {
        List<Member> candidates = memberQueryRepository.findAllByEmailInAndStatus(emails, ACTIVE);
        if (candidates.isEmpty()) {
            return new InvitationFilterResult(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> candidateIds = candidates.stream().map(Member::getId).toList();

        Set<Long> joinedIds = workspaceMemberFinder.getJoinedMemberIds(workspaceKey, candidateIds);
        Set<Long> invitedIds = invitationFinder.findInvitedMemberIds(workspaceKey, candidateIds);

        Map<Boolean, List<Member>> partitioned = candidates.stream()
                .collect(Collectors.partitioningBy(
                        m -> !joinedIds.contains(m.getId()) && !invitedIds.contains(m.getId())));

        List<Member> targets = partitioned.getOrDefault(true, Collections.emptyList());
        List<Member> skipped = partitioned.getOrDefault(false, Collections.emptyList());

        return new InvitationFilterResult(targets, skipped);
    }

    private record InvitationFilterResult(List<Member> targets, List<Member> skipped) {}
}
