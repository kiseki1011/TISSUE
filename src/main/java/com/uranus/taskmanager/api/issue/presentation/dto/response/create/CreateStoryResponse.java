package com.uranus.taskmanager.api.issue.presentation.dto.response.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Story;

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
			.reporterId(story.getReporter())
			.title(story.getTitle())
			.content(story.getContent())
			.summary(story.getSummary())
			.priority(story.getPriority())
			.dueDate(story.getDueDate())
			.userStory(story.getUserStory())
			.acceptanceCriteria(story.getAcceptanceCriteria())
			.difficulty(story.getDifficulty())
			.parentIssueId(story.getParentIssue().getId())
			.build();
	}
}
