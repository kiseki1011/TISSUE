package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateEpicRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	@NotBlank String businessGoal,
	LocalDate targetReleaseDate
) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	@Override
	public void update(Issue issue) {
		Epic epic = (Epic)issue;

		epic.updateTitle(title);
		epic.updateContent(content);
		epic.updateSummary(summary);
		epic.updatePriority(priority);
		epic.updateDueDate(dueDate);
		epic.updateBusinessGoal(businessGoal);
		epic.updateTargetReleaseDate(targetReleaseDate);
	}
}
