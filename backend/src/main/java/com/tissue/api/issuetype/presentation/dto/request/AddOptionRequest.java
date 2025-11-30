package com.tissue.api.issuetype.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.AddOptionCommand;

import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(
	@NotBlank @LabelSize String label
) {
	public AddOptionCommand toCommand(
		String workspaceKey,
		String projectKey,
		Long issueTypeId,
		Long issueFieldId
	) {
		return AddOptionCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.label(Label.of(label))
			.build();
	}
}
