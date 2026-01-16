package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;

public record ManagePositionCommand(
        String workspaceKey, Long targetMemberId, Long positionId, WorkspaceMemberInfo actor) {}
