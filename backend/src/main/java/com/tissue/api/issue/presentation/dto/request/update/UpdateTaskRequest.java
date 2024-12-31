package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Task;

import jakarta.validation.constraints.NotBlank;

public record UpdateTaskRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty
) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}

	@Override
	public void update(Issue issue) {
		Task task = (Task)issue;

		task.updateTitle(title);
		task.updateContent(content);
		task.updateSummary(summary);
		task.updatePriority(priority);
		task.updateDueDate(dueDate);
		task.updateDifficulty(difficulty);
	}
}
