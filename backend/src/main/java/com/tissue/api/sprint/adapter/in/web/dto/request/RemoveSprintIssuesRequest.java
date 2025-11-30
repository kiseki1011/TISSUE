package com.tissue.api.sprint.adapter.in.web.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RemoveSprintIssuesRequest(
	@NotEmpty List<String> issueKeys
) {
}
