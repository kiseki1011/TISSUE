package com.tissue.feature.workspace.application.dto.request;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.util.Set;

public record InviteToWorkspaceCommand(Set<String> emails, WorkspaceRole role, Set<String> targetProjectKeys) {}
