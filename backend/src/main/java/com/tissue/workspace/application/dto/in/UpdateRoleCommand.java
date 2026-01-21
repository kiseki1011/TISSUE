package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public record UpdateRoleCommand(Long targetMemberId, WorkspaceRole grantRole, WorkspaceMemberContext actorContext) {}
