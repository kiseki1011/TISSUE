package com.tissue.api.sprint.presentation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AddSprintIssuesRequest(
	@NotEmpty(message = "{valid.notempty.issuekeys}")
	@Size(max = 100, message = "{valid.size.issuekeys}")
	List<String> issueKeys
) {
}
