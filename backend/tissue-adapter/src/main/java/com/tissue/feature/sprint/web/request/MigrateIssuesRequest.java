package com.tissue.feature.sprint.web.request;

import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Migrate incomplete issues from a completed sprint to another sprint.")
public record MigrateIssuesRequest(
        @NotNull Long newSprintId,

        @Schema(example = "[\"PROJ-10\", \"PROJ-15\", \"PROJ-23\"]") @NotEmpty @Size(max = 100)
        List<String> issueKeys) {

    public MigrateSprintIssuesCommand toCommand() {
        return new MigrateSprintIssuesCommand(newSprintId, issueKeys);
    }
}
