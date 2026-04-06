package com.tissue.feature.workspace.web.request;

import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(
        description = "Invite members to a workspace by email. "
                + "Optionally specify project keys to automatically add invited members to specific projects.")
public record InviteToWorkspaceRequest(
        @Schema(example = "[\"gildong@termissue.dev\", \"bob@termissue.dev\"]") @NotEmpty @Size(max = 50)
        Set<@Email @NotBlank String> emails,

        @Schema(example = "MEMBER") @NotNull WorkspaceRole role,

        @Schema(example = "[\"PROJ-A\", \"PROJ-B\"]") Set<String> targetProjectKeys) {

    public InviteToWorkspaceCommand toCommand() {
        return new InviteToWorkspaceCommand(emails, role, targetProjectKeys);
    }
}
