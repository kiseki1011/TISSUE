package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;

import jakarta.validation.constraints.NotBlank;

public record CreateSubTaskRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	Long parentIssueId

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return SubTask.builder()
			.workspace(workspace)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueDate(dueDate)
			.difficulty(difficulty)
			.parentIssue(parentIssue)
			.build();
	}
}
