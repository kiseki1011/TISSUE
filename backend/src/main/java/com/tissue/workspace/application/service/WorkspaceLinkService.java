package com.tissue.workspace.application.service;

import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.port.in.WorkspaceLinkUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
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

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

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
