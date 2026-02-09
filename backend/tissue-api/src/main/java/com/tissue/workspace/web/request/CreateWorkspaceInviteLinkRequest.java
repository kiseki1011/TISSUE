package com.tissue.workspace.web.request;

import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateWorkspaceInviteLinkRequest(
        @NotNull WorkspaceRole workspaceRole,
        @Nullable List<String> targetProjectKeys,
        @Nullable @Future Instant expiredAt) {

    public CreateWorkspaceInviteLinkCommand toCommand() {
        return new CreateWorkspaceInviteLinkCommand(workspaceRole, targetProjectKeys, expiredAt);
    }
}
