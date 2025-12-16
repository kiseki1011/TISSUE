package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.dto.request.AddOptionCommand;

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
