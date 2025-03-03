package com.tissue.api.issue.presentation.dto.request.create;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record CreateSubTaskRequest(

	@Valid
	CommonIssueFields common

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return SubTask.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.parentIssue(parentIssue)
			.build();
	}
}
