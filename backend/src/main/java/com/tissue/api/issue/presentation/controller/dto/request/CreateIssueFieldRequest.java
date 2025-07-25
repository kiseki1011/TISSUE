package com.tissue.api.issue.presentation.controller.dto.request;

import java.util.List;

import com.tissue.api.issue.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.domain.model.enums.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}")
	String label,
	String description,
	@NotNull(message = "{valid.notnull}")
	FieldType fieldType,
	@NotNull(message = "{valid.notnull}")
	Boolean required,
	List<String> allowedOptions
) {
	public CreateIssueFieldCommand toCommand(String workspaceCode, String issueTypeKey) {
		return CreateIssueFieldCommand.builder()
			.workspaceCode(workspaceCode)
			.issueTypeKey(issueTypeKey)
			.label(label)
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.allowedOptions(allowedOptions)
			.build();
	}
}
