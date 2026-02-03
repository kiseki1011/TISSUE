package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Set;

public record InviteToWorkspaceCommand(
        Set<String> emails, WorkspaceRole role, Set<String> targetProjectKeys, WorkspaceMemberContext actorContext) {}
