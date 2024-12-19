package com.uranus.taskmanager.api.issue.presentation.dto.request;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.IssueType;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;

public record CreateIssueRequest(
	IssueType type,
	@NotBlank(message = "Issue title is required.")
	String title,
	@NotBlank(message = "Issue content is required.")
	String content,
	IssuePriority priority,
	LocalDate dueDate,
	Long parentIssueId
) {
	public Issue to(
		Workspace workspace,
		Issue parentIssue
	) {
		return Issue.builder()
			.workspace(workspace)
			.type(type)
			.title(title)
			.content(content)
			.priority(priority)
			.dueDate(dueDate)
			.parentIssue(parentIssue)
			.build();
	}
}
