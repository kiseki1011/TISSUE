package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.domain.enums.HierarchyLevel;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueTypeRequest(
	@NotBlank @LabelSize String label,
	@StandardText String description,
	@NotNull ColorType color,
	@NotNull HierarchyLevel hierarchyLevel,
	@NotBlank String workflowKey
) {
	public CreateIssueTypeCommand toCommand(String workspaceKey) {
		return CreateIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.label(Label.of(label))
			.description(description)
			.color(color)
			.hierarchyLevel(hierarchyLevel)
			.workflowKey(workflowKey)
			.build();
	}
}
