package com.tissue.api.issue.presentation.dto.request.create;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Task;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record CreateTaskRequest(

	@Valid
	CommonIssueFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}

	@Override
	public Issue toIssue(Workspace workspace) {
		return Task.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.storyPoint(storyPoint)
			.build();
	}
}
