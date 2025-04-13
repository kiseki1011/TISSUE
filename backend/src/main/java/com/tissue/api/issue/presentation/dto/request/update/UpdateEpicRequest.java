package com.tissue.api.issue.presentation.dto.request.update;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;

import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record UpdateEpicRequest(

	@Valid
	CommonIssueUpdateFields common,

	@LongText
	String businessGoal

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	@Override
	public void updateNonNullFields(Issue issue) {
		Epic epic = (Epic)issue;

		if (common.title() != null) {
			epic.updateTitle(common.title());
		}
		if (common.content() != null) {
			epic.updateContent(common.content());
		}

		epic.updateSummary(common.summary());

		if (common.priority() != null) {
			epic.updatePriority(common.priority());
		}
		if (common.dueAt() != null) {
			epic.updateDueAt(common.dueAt());
		}
		if (businessGoal != null) {
			epic.updateBusinessGoal(businessGoal);
		}
	}
}
