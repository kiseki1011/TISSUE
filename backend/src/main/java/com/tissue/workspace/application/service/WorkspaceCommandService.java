package com.tissue.workspace.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.in.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.in.UpdateWorkspaceInfoCommand;
import com.tissue.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceCommandService implements WorkspaceCommandUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public void updateInfo(UpdateWorkspaceInfoCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        Patchers.apply(cmd.name(), workspace::updateName);
        Patchers.apply(cmd.description(), workspace::updateDescription);
    }

    @Override
    public void delete(WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        workspace.softDelete();

        // TODO: WorkspaceDeletedEvent
    }

    @Override
    public void transferOwnership(TransferOwnershipCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        WorkspaceMember originalOwner = workspaceMemberFinder.getActive(actorContext.memberId(), workspace);
        WorkspaceMember newOwner = workspaceMemberFinder.getActive(cmd.targetMemberId(), workspace);

        workspace.transferOwnership(originalOwner, newOwner);

        // TODO: WorkspaceOwnershipTransferredEvent
    }
}
