package com.tissue.feature.sprint.adapter.web.request;

import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Migrate incomplete issues from a completed sprint to another sprint.")
public record MigrateIssuesRequest(@NotNull Long newSprintId) {

    public MigrateSprintIssuesCommand toCommand() {
        return new MigrateSprintIssuesCommand(newSprintId);
    }
}
