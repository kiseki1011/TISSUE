package com.tissue.workspace.adapter.web.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import jakarta.validation.constraints.Future;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CreateProjectInviteLinkRequest(
        @Nullable @Future Instant expiredAt) {

    public CreateProjectInviteLinkCommand toCommand(String projectKey, WorkspaceMemberContext actorContext) {
        return new CreateProjectInviteLinkCommand(projectKey, expiredAt, actorContext);
    }
}
