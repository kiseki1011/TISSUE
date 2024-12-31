package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;
import java.util.Set;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateBugRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	@NotBlank String reproducingSteps,
	@NotNull BugSeverity severity,
	Set<String> affectedVersions
) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}

	@Override
	public void update(Issue issue) {
		Bug bug = (Bug)issue;

		bug.updateTitle(title);
		bug.updateContent(content);
		bug.updateSummary(summary);
		bug.updatePriority(priority);
		bug.updateDueDate(dueDate);
		bug.updateDifficulty(difficulty);
		bug.updateReproducingSteps(reproducingSteps);
		bug.updateSeverity(severity);
		bug.updateAffectedVersions(affectedVersions);
	}
}
