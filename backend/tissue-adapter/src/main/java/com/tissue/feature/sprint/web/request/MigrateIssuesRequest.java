package com.tissue.feature.sprint.web.request;

import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MigrateIssuesRequest(
        @NotNull Long newSprintId,
        @NotEmpty @Size(max = 100) List<String> issueKeys) {

    public MigrateSprintIssuesCommand toCommand() {
        return new MigrateSprintIssuesCommand(newSprintId, issueKeys);
    }
}
