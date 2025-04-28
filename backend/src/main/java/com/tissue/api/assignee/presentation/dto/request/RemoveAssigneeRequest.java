package com.tissue.api.assignee.presentation.dto.request;

import com.tissue.api.assignee.service.dto.RemoveAssigneeCommand;

import jakarta.validation.constraints.NotNull;

public record RemoveAssigneeRequest(
	@NotNull(message = "{valid.notnull}")
	Long memberId
) {
	public RemoveAssigneeCommand toCommand() {
		return new RemoveAssigneeCommand(memberId);
	}
}
