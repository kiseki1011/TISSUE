package com.tissue.workspace.adapter.web.request;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CreateProjectInviteLinkRequest(
        @NotNull ProjectRole role, @Nullable @Future Instant expiredAt) {

    public CreateProjectInviteLinkCommand toCommand(String projectKey, WorkspaceMemberContext actorContext) {
        return new CreateProjectInviteLinkCommand(projectKey, role, expiredAt, actorContext);
    }
}
