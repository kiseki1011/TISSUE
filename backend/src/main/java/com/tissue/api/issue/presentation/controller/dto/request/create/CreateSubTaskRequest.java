package com.tissue.api.issue.presentation.controller.dto.request.create;

import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.types.SubTask;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record CreateSubTaskRequest(

	@Valid
	CommonIssueCreateFields common

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}

	@Override
	public Issue toIssue(Workspace workspace) {
		return SubTask.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.build();
	}
}
