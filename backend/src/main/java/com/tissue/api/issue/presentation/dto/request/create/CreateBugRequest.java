package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDate;
import java.util.Set;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;

import jakarta.validation.constraints.NotBlank;

public record CreateBugRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	Long parentIssueId,
	@NotBlank String reproducingSteps,
	BugSeverity severity,
	Set<String> affectedVersions
) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Bug.builder()
			.workspace(workspace)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueDate(dueDate)
			.difficulty(difficulty)
			.parentIssue(parentIssue)
			.reproducingSteps(reproducingSteps)
			.severity(severity)
			.affectedVersions(affectedVersions)
			.build();
	}
}
