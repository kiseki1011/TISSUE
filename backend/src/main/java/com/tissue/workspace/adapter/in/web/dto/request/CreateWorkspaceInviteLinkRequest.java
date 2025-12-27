package com.tissue.workspace.adapter.in.web.dto.request;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.lang.Nullable;

public record CreateWorkspaceInviteLinkRequest(
        @NotNull WorkspaceRole workspaceRole,
        @Nullable List<ProjectJoinConfigDto> targetProjects,
        @Nullable @Future Instant expiredAt) {
    public CreateWorkspaceInviteLinkCommand toCommand(String workspaceKey) {
        return new CreateWorkspaceInviteLinkCommand(
                workspaceKey, workspaceRole, targetProjects, expiredAt);
    }
}
