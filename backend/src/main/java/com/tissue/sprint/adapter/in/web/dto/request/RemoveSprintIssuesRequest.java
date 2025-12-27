package com.tissue.sprint.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RemoveSprintIssuesRequest(@NotEmpty List<String> issueKeys) {}
