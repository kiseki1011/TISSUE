package com.tissue.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
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
