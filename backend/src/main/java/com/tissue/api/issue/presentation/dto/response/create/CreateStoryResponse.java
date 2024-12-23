package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;

import lombok.Builder;

@Builder
public record CreateStoryResponse(
	Long issueId,
	String workspaceCode,
	Long reporterId,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	String userStory,
	String acceptanceCriteria,
	Difficulty difficulty,
	Long parentIssueId
) implements CreateIssueResponse {

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}

	public static CreateStoryResponse from(Story story) {
		return CreateStoryResponse.builder()
			.issueId(story.getId())
			.workspaceCode(story.getWorkspaceCode())
			.reporterId(story.getCreatedBy())
			.title(story.getTitle())
			.content(story.getContent())
			.summary(story.getSummary())
			.priority(story.getPriority())
			.dueDate(story.getDueDate())
			.userStory(story.getUserStory())
			.acceptanceCriteria(story.getAcceptanceCriteria())
			.difficulty(story.getDifficulty())
			.parentIssueId(Optional.ofNullable(story.getParentIssue())
				.map(Issue::getId)
				.orElse(null))
			.build();
	}
}
