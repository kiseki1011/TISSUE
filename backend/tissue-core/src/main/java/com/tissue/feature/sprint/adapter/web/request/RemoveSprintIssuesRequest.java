package com.tissue.feature.sprint.adapter.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RemoveSprintIssuesRequest(
        @NotEmpty @Size(max = 100) List<String> issueKeys) {}
