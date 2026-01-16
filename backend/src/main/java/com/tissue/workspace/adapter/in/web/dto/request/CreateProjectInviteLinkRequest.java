package com.tissue.workspace.adapter.in.web.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CreateProjectInviteLinkRequest(
        @NotNull ProjectRole role, @Nullable @Future Instant expiredAt) {
    public CreateProjectInviteLinkCommand toCommand(String workspaceKey, String projectKey, WorkspaceMemberInfo actor) {
        return new CreateProjectInviteLinkCommand(workspaceKey, projectKey, role, expiredAt, actor);
    }
}
