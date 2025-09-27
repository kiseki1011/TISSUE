package com.tissue.api.issue.base.presentation.dto.request;

import org.springframework.lang.Nullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.domain.enums.HierarchyLevel;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueTypeRequest(
	@NotBlank @LabelSize String label,
	@Nullable @Size(max = 255) String description,
	@NotNull ColorType color,
	@NotNull HierarchyLevel hierarchyLevel,
	@NotNull Long workflowId
) {
	public CreateIssueTypeCommand toCommand(String workspaceKey) {
		return CreateIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.label(Label.of(label))
			.description(description)
			.color(color)
			.hierarchyLevel(hierarchyLevel)
			.workflowId(workflowId)
			.build();
	}
}
