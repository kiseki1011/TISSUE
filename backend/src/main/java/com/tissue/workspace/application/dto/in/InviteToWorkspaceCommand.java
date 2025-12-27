package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Set;

public record InviteToWorkspaceCommand(
        Set<String> emails,
        String workspaceKey,
        WorkspaceRole role,
        Set<ProjectJoinConfigDto> targetProjects) {}
