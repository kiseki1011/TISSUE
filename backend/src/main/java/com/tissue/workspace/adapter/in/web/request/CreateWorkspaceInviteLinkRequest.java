package com.tissue.workspace.adapter.in.web.request;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateWorkspaceInviteLinkRequest(
        @NotNull WorkspaceRole workspaceRole,
        @Nullable List<ProjectJoinConfigDto> targetProjects,
        @Nullable @Future Instant expiredAt) {

    public CreateWorkspaceInviteLinkCommand toCommand(WorkspaceMemberContext actor) {
        return new CreateWorkspaceInviteLinkCommand(workspaceRole, targetProjects, expiredAt, actor);
    }
}
