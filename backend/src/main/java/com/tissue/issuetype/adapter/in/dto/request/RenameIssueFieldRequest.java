package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameIssueFieldCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueFieldRequest(
	@NotBlank @LabelSize String name
) {
	public RenameIssueFieldCommand toCommand(
		String workspaceKey,
		String projectKey,
		Long issueTypeId,
		Long issueFieldId
	) {
		return RenameIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.name(Name.of(name))
			.build();
	}
}
