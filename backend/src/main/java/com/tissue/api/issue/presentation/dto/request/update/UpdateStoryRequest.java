package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateStoryRequest(

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

	@LongText
	@NotBlank(message = "{valid.notblank}")
	String userStory,

	@LongText
	String acceptanceCriteria

) implements UpdateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}

	@Override
	public void update(Issue issue) {
		Story story = (Story)issue;

		story.updateTitle(title);
		story.updateContent(content);
		story.updateSummary(summary);
		story.updatePriority(priority);
		story.updateDueDate(dueDate);
		story.updateDifficulty(difficulty);
		story.updateUserStory(userStory);
		story.updateAcceptanceCriteria(acceptanceCriteria);
	}
}
