package com.tissue.api.issuetype.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issuetype.application.dto.RenameIssueTypeCommand;
import com.tissue.api.issue.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(
	@NotBlank @LabelSize String label
) {
	public RenameIssueTypeCommand toCommand(String workspaceKey, Long id) {
		return RenameIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.id(id)
			.label(Label.of(label))
			.build();
	}
}
