package com.tissue.api.assignee.presentation.dto.request;

import com.tissue.api.assignee.service.dto.AddAssigneeCommand;

import jakarta.validation.constraints.NotNull;

public record AddAssigneeRequest(

	@NotNull(message = "{valid.notnull}")
	Long memberId
) {
	public AddAssigneeCommand toCommand() {
		return new AddAssigneeCommand(memberId);
	}
}
