package com.tissue.sprint.adapter.in.web.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AddSprintIssuesRequest(
	@NotEmpty @Size(max = 100, message = "Cannot add more than 100 issues to the sprint.")
	List<String> issueKeys
) {
}
