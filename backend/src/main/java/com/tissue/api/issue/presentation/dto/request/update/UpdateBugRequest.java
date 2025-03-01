package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;
import java.util.Set;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateBugRequest(

	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String title,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String content,

	@StandardText
	String summary,

	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String reproducingSteps,

	BugSeverity severity,
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
		bug.updateReproducingSteps(reproducingSteps);
		bug.updateSeverity(severity);
		bug.updateAffectedVersions(affectedVersions);
	}
}
