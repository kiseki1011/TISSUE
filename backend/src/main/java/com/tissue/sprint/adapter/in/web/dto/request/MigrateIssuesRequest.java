package com.tissue.sprint.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MigrateIssuesRequest(
        @NotNull Long newSprintId, @NotEmpty @Size(max = 100) List<String> issueKeys) {}
