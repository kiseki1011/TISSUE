package com.tissue.api.issue.presentation.controller.dto.request.create;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.types.Epic;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateEpicRequest(

	@Valid
	CommonIssueCreateFields common,

	@LongText
	@NotBlank(message = "{valid.notblank}")
	String businessGoal

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	@Override
	public Issue toIssue(Workspace workspace) {
		return Epic.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.businessGoal(businessGoal)
			.build();
	}
}

