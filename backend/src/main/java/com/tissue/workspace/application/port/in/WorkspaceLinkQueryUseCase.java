package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;

public interface WorkspaceLinkQueryUseCase {

    WorkspaceInviteLinkDetail getLinkDetail(String token, WorkspaceMemberContext actor);

    // TODO: getWorkspaceLinks
    //  all active links for the workspace
}
