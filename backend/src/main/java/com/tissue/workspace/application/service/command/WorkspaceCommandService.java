package com.tissue.workspace.application.service.command;

import com.tissue.common.util.Patchers;
import com.tissue.workspace.application.dto.in.DeleteWorkspaceCommand;
import com.tissue.workspace.application.dto.in.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.in.UpdateWorkspaceInfoCommand;
import com.tissue.workspace.application.port.in.WorkspaceCommandUseCase;
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

    @Override
    public void updateInfo(UpdateWorkspaceInfoCommand cmd) {
        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        Patchers.apply(cmd.name(), workspace::updateName);
        Patchers.apply(cmd.description(), workspace::updateDescription);
    }

    @Override
    public void delete(DeleteWorkspaceCommand cmd) {
        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        workspace.softDelete();

        // TODO: WorkspaceDeletedEvent
        //  - when sending notifications the target should be Members not WorkspaceMembers
    }

    @Override
    public void transferOwnership(TransferOwnershipCommand cmd) {
        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        WorkspaceMember originalOwner =
                workspaceMemberFinder.findBy(cmd.actorMemberId(), workspace);
        WorkspaceMember newOwner = workspaceMemberFinder.findBy(cmd.targetMemberId(), workspace);

        workspace.transferOwnership(originalOwner, newOwner);

        // TODO: WorkspaceOwnershipTransferredEvent
    }
}
