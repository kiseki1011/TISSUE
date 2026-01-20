package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateWorkspaceInviteLinkCommand(
        WorkspaceRole workspaceRole,
        @Nullable List<ProjectJoinConfigDto> targetProjects,
        @Nullable Instant expiredAt,
        WorkspaceMemberContext actorContext) {}
