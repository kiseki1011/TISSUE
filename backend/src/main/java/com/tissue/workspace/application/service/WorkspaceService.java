package com.tissue.workspace.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
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
public class WorkspaceService implements WorkspaceCommandUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public void updateInfo(UpdateWorkspaceInfoCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        Patchers.apply(cmd.name(), workspace::updateName);
        Patchers.apply(cmd.description(), workspace::updateDescription);
    }

    @Override
    public void delete(WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        workspace.softDelete();

        // TODO: 하위 project들도 cascade soft-delete 처리

        // TODO: WorkspaceDeletedEvent
        //   - Should i send notifications though?
    }

    @Override
    public void transferOwnership(Long targetMemberId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        WorkspaceMember originalOwner = workspaceMemberFinder.getBy(workspace, actorContext.memberId());
        WorkspaceMember newOwner = workspaceMemberFinder.getBy(workspace, targetMemberId);

        workspace.transferOwnership(originalOwner, newOwner);

        // TODO: WorkspaceOwnershipTransferredEvent
    }
}
