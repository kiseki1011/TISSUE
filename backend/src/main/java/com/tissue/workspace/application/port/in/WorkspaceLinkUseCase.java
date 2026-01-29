package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

    String createProjectLink(CreateProjectInviteLinkCommand cmd);

    void expireLink(ExpireLinkCommand cmd);

    //    WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);
}
