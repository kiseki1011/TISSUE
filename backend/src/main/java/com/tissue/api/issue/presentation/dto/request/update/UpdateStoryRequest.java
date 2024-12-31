package com.tissue.api.issue.presentation.dto.request.update;

import java.time.LocalDate;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;

import jakarta.validation.constraints.NotBlank;

public record UpdateStoryRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	String userStory,
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
