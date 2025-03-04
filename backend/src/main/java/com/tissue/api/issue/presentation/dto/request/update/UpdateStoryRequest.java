package com.tissue.api.issue.presentation.dto.request.update;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record UpdateStoryRequest(

	@Valid
	CommonIssueUpdateFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint,

	@LongText
	String userStory,

	@LongText
	String acceptanceCriteria

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}

	@Override
	public void updateNonNullFields(Issue issue) {
		Story story = (Story)issue;

		if (common.title() != null) {
			story.updateTitle(common.title());
		}
		if (common.content() != null) {
			story.updateContent(common.content());
		}

		story.updateSummary(common.summary());

		if (common.priority() != null) {
			story.updatePriority(common.priority());
		}
		if (common.dueAt() != null) {
			story.updateDueAt(common.dueAt());
		}

		story.updateStoryPoint(storyPoint);

		if (userStory != null) {
			story.updateUserStory(userStory);
		}
		if (acceptanceCriteria != null) {
			story.updateAcceptanceCriteria(acceptanceCriteria);
		}
	}
}
