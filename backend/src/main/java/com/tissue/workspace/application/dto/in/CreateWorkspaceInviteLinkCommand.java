package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateWorkspaceInviteLinkCommand(
        String workspaceKey,
        WorkspaceRole workspaceRole,
        List<ProjectJoinConfigDto> targetProjects,
        @Nullable Instant expiredAt) {}
