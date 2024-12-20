package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;

import jakarta.validation.constraints.NotBlank;

public record EpicCreateRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Long parentIssueId,
	@NotBlank String businessGoal,
	LocalDate targetReleaseDate,
	LocalDate hardDeadLine
) implements IssueCreateRequest {
	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}
}
