package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

    String createProjectLink(CreateProjectInviteLinkCommand cmd);

    void expireLink(ExpireLinkCommand cmd);

    WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);

    WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, WorkspaceMemberInfo actor);

    // TODO: getWorkspaceLinks
    //  all active links for the workspace
}
