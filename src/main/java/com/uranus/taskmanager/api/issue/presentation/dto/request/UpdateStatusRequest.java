package com.uranus.taskmanager.api.issue.presentation.dto.request;

import com.uranus.taskmanager.api.issue.domain.IssueStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
	@NotNull
	IssueStatus status
) {
}
