package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import java.util.List;

public interface WorkspaceLinkQueryUseCase {

    WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, Long actorMemberId);

    List<WorkspaceInviteLinkDetail> getWorkspaceLinks(String workspaceKey, Long actorMemberId);
}
