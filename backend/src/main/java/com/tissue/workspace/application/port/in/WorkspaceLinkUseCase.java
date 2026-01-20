package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

    String createProjectLink(CreateProjectInviteLinkCommand cmd);

    void expireLink(ExpireLinkCommand cmd);

    //    WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);
}
