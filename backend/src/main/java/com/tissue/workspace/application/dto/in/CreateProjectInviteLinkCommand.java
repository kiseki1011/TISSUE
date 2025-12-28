package com.tissue.workspace.application.dto.in;

import com.tissue.project.domain.enums.ProjectRole;
import java.time.Instant;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateProjectInviteLinkCommand(
        String workspaceKey, String projectKey, ProjectRole role, @Nullable Instant expiredAt) {}
