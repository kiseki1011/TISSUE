package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issue.base.application.dto.RenameOptionCommand;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(
	@NotBlank @LabelSize String label
) {
	public RenameOptionCommand toCommand(
		String workspaceKey,
		Long issueTypeId,
		Long issueFieldId,
		Long optionId
	) {
		return RenameOptionCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.optionId(optionId)
			.label(Label.of(label))
			.build();
	}
}
