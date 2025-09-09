package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.AddOptionCommand;

import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(
	@NotBlank(message = "{valid.notblank}") String label
) {
	public AddOptionCommand toCommand(String workspaceKey, String issueTypeKey, String issueFieldKey) {
		return AddOptionCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.issueFieldKey(issueFieldKey)
			.label(TextNormalizer.normalizeLabel(label))
			.build();
	}
}
