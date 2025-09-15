package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.common.util.CollectionNormalizer;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.domain.enums.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}") String label,
	@StandardText String description,
	@NotNull(message = "{valid.notnull}") FieldType fieldType,
	@NotNull(message = "{valid.notnull}") Boolean required,
	List<String> initialOptions
) {
	public CreateIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId) {
		return CreateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.label(TextNormalizer.normalizeLabel(label))
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.initialOptions(CollectionNormalizer.normalizeOptions(initialOptions))
			.build();
	}
}
