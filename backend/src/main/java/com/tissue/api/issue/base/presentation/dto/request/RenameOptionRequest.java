package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.RenameOptionCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(
	@NotBlank(message = "{valid.notblank}") String newLabel
) {
	public RenameOptionCommand toCommand(
		String workspaceKey,
		String issueTypeKey,
		String issueFieldKey,
		String optionKey
	) {
		return RenameOptionCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.issueFieldKey(issueFieldKey)
			.optionKey(optionKey)
			.newLabel(TextNormalizer.normalizeLabel(newLabel))
			.build();
	}
}
