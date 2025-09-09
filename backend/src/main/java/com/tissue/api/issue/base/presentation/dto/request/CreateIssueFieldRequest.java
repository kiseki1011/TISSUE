package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.common.util.CollectionNormalizer;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.domain.enums.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}") String label,
	String description,
	@NotNull(message = "{valid.notnull}") FieldType fieldType,
	@NotNull(message = "{valid.notnull}") Boolean required,
	List<String> initialOptions
) {
	public CreateIssueFieldCommand toCommand(String workspaceKey, String issueTypeKey) {
		return CreateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.label(TextNormalizer.normalizeLabel(label))
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.initialOptions(CollectionNormalizer.normalizeOptions(initialOptions))
			.build();
	}
}
