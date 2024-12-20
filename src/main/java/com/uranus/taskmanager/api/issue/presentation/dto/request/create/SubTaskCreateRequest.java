package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;

import jakarta.validation.constraints.NotBlank;

public record SubTaskCreateRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Long parentIssueId,
	Difficulty difficulty

) implements IssueCreateRequest {

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}
}
