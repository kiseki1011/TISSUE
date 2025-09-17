package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issue.base.application.dto.RenameIssueFieldCommand;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueFieldRequest(
	@NotBlank @LabelSize String label
) {
	public RenameIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		return RenameIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.label(Label.of(label))
			.build();
	}
}
