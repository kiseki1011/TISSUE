package com.tissue.sprint.adapter.in.web.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MigrateIssuesRequest(
        @NotNull Long newSprintId,
        @NotEmpty @Size(max = 100) List<String> issueKeys) {

    public MigrateSprintIssuesCommand toCommand(Long originalSprintId, ProjectMemberContext actorContext) {
        return new MigrateSprintIssuesCommand(originalSprintId, newSprintId, issueKeys, actorContext);
    }
}
