package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public record UpdateRoleCommand(
        String workspaceKey, Long targetMemberId, WorkspaceRole role, WorkspaceMemberInfo actor) {}
