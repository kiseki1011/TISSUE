package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;

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
