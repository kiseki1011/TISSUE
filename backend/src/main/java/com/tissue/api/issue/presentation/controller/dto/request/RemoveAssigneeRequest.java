package com.tissue.api.issue.presentation.controller.dto.request;

import com.tissue.api.issue.application.dto.RemoveAssigneeCommand;

import jakarta.validation.constraints.NotNull;

public record RemoveAssigneeRequest(
	@NotNull(message = "{valid.notnull}")
	Long memberId
) {
	public RemoveAssigneeCommand toCommand() {
		return new RemoveAssigneeCommand(memberId);
	}
}
