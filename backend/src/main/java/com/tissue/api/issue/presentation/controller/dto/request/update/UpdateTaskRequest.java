package com.tissue.api.issue.presentation.controller.dto.request.update;

import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.types.Task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record UpdateTaskRequest(

	@Valid
	CommonIssueUpdateFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}

	@Override
	public void updateNonNullFields(Issue issue) {
		Task task = (Task)issue;

		if (common.title() != null) {
			task.updateTitle(common.title());
		}
		if (common.content() != null) {
			task.updateContent(common.content());
		}

		task.updateSummary(common.summary());

		if (common.priority() != null) {
			task.updatePriority(common.priority());
		}
		if (common.dueAt() != null) {
			task.updateDueAt(common.dueAt());
		}

		task.updateStoryPoint(storyPoint);
	}
}
