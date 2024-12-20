package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;
import java.util.Set;

import com.uranus.taskmanager.api.issue.domain.enums.BugSeverity;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;

import jakarta.validation.constraints.NotBlank;

public record BugCreateRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Long parentIssueId,
	@NotBlank String reproducingSteps,
	BugSeverity severity,
	Set<String> affectedVersions,
	Difficulty difficulty
) implements IssueCreateRequest {

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}
}
