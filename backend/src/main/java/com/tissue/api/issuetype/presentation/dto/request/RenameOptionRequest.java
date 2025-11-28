package com.tissue.api.issuetype.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.RenameOptionCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(
	@NotBlank @LabelSize String label
) {
	public RenameOptionCommand toCommand(
		String workspaceKey,
		String projectKey,
		Long issueTypeId,
		Long issueFieldId,
		Long optionId
	) {
		return RenameOptionCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.optionId(optionId)
			.label(Label.of(label))
			.build();
	}
}
