package com.tissue.api.issue.presentation.controller.dto.request;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.domain.model.enums.HierarchyLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateIssueTypeRequest(
	@NotBlank(message = "{valid.notblank}")
	String label,
	@NotNull(message = "{valid.notnull}")
	ColorType color,
	@NotNull(message = "{valid.notnull}")
	HierarchyLevel hierarchyLevel,
	@NotBlank(message = "{valid.notblank}")
	String workflowKey
) {
	public CreateIssueTypeCommand toCommand(String workspaceCode) {
		return CreateIssueTypeCommand.builder()
			.workspaceCode(workspaceCode)
			.label(label)
			.color(color)
			.hierarchyLevel(hierarchyLevel)
			.workflowKey(workflowKey)
			.build();
	}
}
