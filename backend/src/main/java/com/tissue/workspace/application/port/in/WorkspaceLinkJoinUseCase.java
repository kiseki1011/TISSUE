package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.response.command.WorkspaceMemberResponse;

public interface WorkspaceLinkJoinUseCase {

    WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId);
}
