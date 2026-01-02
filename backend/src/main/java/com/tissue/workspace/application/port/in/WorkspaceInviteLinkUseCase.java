package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

    String createProjectLink(CreateProjectInviteLinkCommand cmd);

    void expireLink(ExpireLinkCommand cmd);

    WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);

    // TODO: getLinkInfo -> getLinkDetail
    WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);

    // TODO: getWorkspaceLinks
    //  all active links for the workspace
}
