package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issue.base.application.dto.RenameIssueTypeCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(
	@NotBlank(message = "{valid.notblank}") @LabelSize String label
) {
	public RenameIssueTypeCommand toCommand(String workspaceKey, Long id) {
		return RenameIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.id(id)
			.label(TextNormalizer.normalizeLabel(label))
			.build();
	}
}
