package com.tissue.api.issue.presentation.controller.dto.request.update;

import java.util.Set;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.issue.domain.model.enums.BugSeverity;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.types.Bug;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record UpdateBugRequest(

	@Valid
	CommonIssueUpdateFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint,

	@ContentText
	String reproducingSteps,

	BugSeverity severity,
	Set<String> affectedVersions

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}

	@Override
	public void updateNonNullFields(Issue issue) {
		Bug bug = (Bug)issue;

		if (common.title() != null) {
			bug.updateTitle(common.title());
		}
		if (common.content() != null) {
			bug.updateContent(common.content());
		}

		bug.updateSummary(common.summary());

		if (common.priority() != null) {
			bug.updatePriority(common.priority());
		}
		if (common.dueAt() != null) {
			bug.updateDueAt(common.dueAt());
		}

		bug.updateStoryPoint(storyPoint);

		if (reproducingSteps != null) {
			bug.updateReproducingSteps(reproducingSteps);
		}
		if (severity != null) {
			bug.updateSeverity(severity);
		}
		if (affectedVersions != null) {
			bug.updateAffectedVersions(affectedVersions);
		}
	}
}
