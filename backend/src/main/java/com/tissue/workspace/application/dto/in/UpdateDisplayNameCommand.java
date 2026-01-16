package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;

public record UpdateDisplayNameCommand(
        String workspaceKey, Long targetMemberId, String displayName, WorkspaceMemberInfo actor) {}
