package com.tissue.feature.workspace.web.request;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateWorkspaceInviteLinkRequest(
        @Schema(description = "Workspace role assigned to members who join via this link") @NotNull
        WorkspaceRole workspaceRole,

        @Schema(
                description = "Project keys to automatically add the invited member to. "
                        + "If `null`, the member only joins the workspace.")
        @Nullable
        List<String> targetProjectKeys,

        @Nullable @Future Instant expiredAt) {

    public CreateWorkspaceInviteLinkCommand toCommand() {
        return new CreateWorkspaceInviteLinkCommand(workspaceRole, targetProjectKeys, expiredAt);
    }
}
