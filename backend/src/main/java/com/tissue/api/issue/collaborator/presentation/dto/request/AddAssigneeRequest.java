package com.tissue.api.issue.collaborator.presentation.dto.request;

import com.tissue.api.issue.collaborator.application.dto.AddAssigneeCommand;

import jakarta.validation.constraints.NotNull;

public record AddAssigneeRequest(

	@NotNull(message = "{valid.notnull}")
	Long memberId
) {
	public AddAssigneeCommand toCommand() {
		return new AddAssigneeCommand(memberId);
	}
}
