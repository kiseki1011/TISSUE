package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Epic;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;

public record CreateEpicRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Long parentIssueId,
	@NotBlank String businessGoal,
	LocalDate targetReleaseDate,
	LocalDate hardDeadLine
) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Epic.builder()
			.workspace(workspace)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueDate(dueDate)
			.businessGoal(businessGoal)
			.targetReleaseDate(targetReleaseDate)
			.hardDeadLine(hardDeadLine)
			.build();
	}
}
