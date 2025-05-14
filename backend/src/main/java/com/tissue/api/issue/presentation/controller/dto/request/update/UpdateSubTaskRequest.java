package com.tissue.api.issue.presentation.controller.dto.request.update;

import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.types.SubTask;

import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record UpdateSubTaskRequest(

	@Valid
	CommonIssueUpdateFields common

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}

	@Override
	public void updateNonNullFields(Issue issue) {
		SubTask subTask = (SubTask)issue;

		if (common.title() != null) {
			subTask.updateTitle(common.title());
		}
		if (common.content() != null) {
			subTask.updateContent(common.content());
		}

		subTask.updateSummary(common.summary());

		if (common.priority() != null) {
			subTask.updatePriority(common.priority());
		}
		if (common.dueAt() != null) {
			subTask.updateDueAt(common.dueAt());
		}
	}
}
