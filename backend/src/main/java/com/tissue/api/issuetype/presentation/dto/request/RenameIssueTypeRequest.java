package com.tissue.api.issuetype.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.RenameIssueTypeCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(
	@NotBlank @LabelSize String label
) {
	public RenameIssueTypeCommand toCommand(String workspaceKey, String projectKey, Long id) {
		return RenameIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.id(id)
			.label(Label.of(label))
			.build();
	}
}
