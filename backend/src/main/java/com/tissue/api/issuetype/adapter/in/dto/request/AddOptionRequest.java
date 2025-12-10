package com.tissue.api.issuetype.adapter.in.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.request.AddOptionCommand;

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
