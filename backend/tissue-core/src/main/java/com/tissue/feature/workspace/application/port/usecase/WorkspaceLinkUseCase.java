package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import java.util.List;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(String workspaceKey, CreateWorkspaceInviteLinkCommand cmd, Long actorMemberId);

    void deleteLink(String workspaceKey, String token, Long actorMemberId);

    WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId);

    WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, Long actorMemberId);

    List<WorkspaceInviteLinkDetail> getWorkspaceLinks(String workspaceKey, Long actorMemberId);
}
