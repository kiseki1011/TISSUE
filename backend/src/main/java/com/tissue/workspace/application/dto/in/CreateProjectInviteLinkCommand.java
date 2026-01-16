package com.tissue.workspace.application.dto.in;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateProjectInviteLinkCommand(
        String workspaceKey,
        String projectKey,
        ProjectRole role,
        @Nullable Instant expiredAt,
        WorkspaceMemberInfo actor) {}
