package com.tissue.api.issuetype.adapter.in.dto.request;

import org.springframework.lang.Nullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issuetype.application.dto.request.CreateIssueTypeCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueTypeRequest(
	@NotBlank @LabelSize String label,
	@Nullable @Size(max = 255) String description,
	@NotNull ColorType color,
	@NotNull IssueHierarchy issueHierarchy,
	@NotNull Long workflowId
) {
	public CreateIssueTypeCommand toCommand(String workspaceKey, String projectKey) {
		return CreateIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.label(Label.of(label))
			.description(description)
			.color(color)
			.issueHierarchy(issueHierarchy)
			.workflowId(workflowId)
			.build();
	}
}
