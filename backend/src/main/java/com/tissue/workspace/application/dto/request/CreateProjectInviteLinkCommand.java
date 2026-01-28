package com.tissue.workspace.application.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CreateProjectInviteLinkCommand(
        String projectKey, ProjectRole role, @Nullable Instant expiredAt, WorkspaceMemberContext actorContext) {}
