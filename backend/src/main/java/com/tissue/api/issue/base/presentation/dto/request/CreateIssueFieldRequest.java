package com.tissue.api.issue.base.presentation.dto.request;

import java.util.List;

import com.tissue.api.common.util.CollectionNormalizer;
import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueFieldRequest(
	@NotBlank @LabelSize String label,
	@StandardText String description,
	@NotNull FieldType fieldType,
	@NotNull Boolean required,
	List<String> initialOptions
) {
	public CreateIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId) {
		return CreateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.label(Label.of(label))
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.initialOptions(CollectionNormalizer.normalizeOptions(initialOptions))
			.build();
	}
}
