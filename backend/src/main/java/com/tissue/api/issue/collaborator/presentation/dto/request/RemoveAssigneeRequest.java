package com.tissue.api.issue.collaborator.presentation.dto.request;

import com.tissue.api.issue.collaborator.application.dto.RemoveAssigneeCommand;

import jakarta.validation.constraints.NotNull;

public record RemoveAssigneeRequest(
	@NotNull(message = "{valid.notnull}")
	Long memberId
) {
	public RemoveAssigneeCommand toCommand() {
		return new RemoveAssigneeCommand(memberId);
	}
}
