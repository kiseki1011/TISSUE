package com.tissue.feature.workspace.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.service.ProjectJoinService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.repository.WorkspaceLinkCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceLinkQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.InvalidWorkspaceInviteLinkException;
import com.tissue.feature.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceLinkService implements WorkspaceLinkUseCase {

    private final MemberFinder memberFinder;
    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceParticipationService workspaceParticipationService;
    private final ProjectJoinService projectJoinService;

    @Override
    public String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        return saveLink(workspace, cmd.workspaceRole(), cmd.targetProjectKeys(), cmd.expiredAt());
    }

    @Override
    public void expireLink(String token, WorkspaceMemberContext actorContext) {
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(actorContext.workspaceKey(), token));

        workspaceAuthorizationService.requireInviteLinkEditPermission(link, actorContext);

        link.expire();
    }

    private String saveLink(
            Workspace workspace,
            WorkspaceRole roleToGrant,
            @Nullable List<String> projectKeys,
            @Nullable Instant expiredAt) {

        String token = UUID.randomUUID().toString();
        WorkspaceInviteLink link = WorkspaceInviteLink.create(workspace, token, roleToGrant, expiredAt);

        addProjectsToLink(workspace.getKey(), projectKeys, link);

        linkRepository.save(link);
        return token;
    }

    @Override
    public WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId) {
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        if (!link.isValid()) {
            throw new InvalidWorkspaceInviteLinkException(link);
        }

        WorkspaceMember workspaceMember = workspaceParticipationService.join(
                link.getWorkspace(), memberFinder.getActiveBy(actorMemberId), link.getWorkspaceRole());

        List<String> projectKeys = link.getProjectKeys();

        if (link.projectKeysNotEmpty()) {
            joinProjects(projectKeys, workspaceMember);
        }

        // TODO: eventPublisher.publishJoinedViaLink

        return WorkspaceMemberResponse.from(workspaceMember);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceInviteLinkDetail getLinkDetail(String token, WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceMember(actorContext);

        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(actorContext.workspaceKey(), token));

        WorkspaceMember linkCreator = workspaceMemberFinder.getBy(actorContext.workspaceKey(), link.getCreatedBy());

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }

    private void joinProjects(List<String> projectKeys, WorkspaceMember workspaceMember) {
        for (var projectKey : projectKeys) {
            projectFinder
                    .getOptionalBy(workspaceMember.getWorkspaceKey(), projectKey)
                    .ifPresent(project -> {
                        projectJoinService.join(project, workspaceMember);
                    });
        }
    }

    private void addProjectsToLink(String workspaceKey, @Nullable List<String> projectKeys, WorkspaceInviteLink link) {
        if (projectKeys == null) {
            return;
        }
        for (var projectKey : projectKeys) {
            projectFinder.getWithWorkspaceBy(workspaceKey, projectKey);
            link.addProjectKey(projectKey);
        }
    }
}
