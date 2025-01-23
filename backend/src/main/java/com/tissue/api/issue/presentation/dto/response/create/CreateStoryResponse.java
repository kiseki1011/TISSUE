package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
	String issueKey,
	String workspaceCode,

	Long createrId,
	LocalDateTime createdAt,

	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,

	String userStory,
	String acceptanceCriteria,
	Difficulty difficulty,

	String parentIssueKey

) implements CreateIssueResponse {

	public static CreateStoryResponse from(Story story) {
		return CreateStoryResponse.builder()
			.issueId(story.getId())
			.issueKey(story.getIssueKey())
			.workspaceCode(story.getWorkspaceCode())
			.createrId(story.getCreatedByWorkspaceMember())
			.createdAt(story.getCreatedDate())
			.title(story.getTitle())
			.content(story.getContent())
			.summary(story.getSummary())
			.priority(story.getPriority())
			.dueDate(story.getDueDate())
			.userStory(story.getUserStory())
			.acceptanceCriteria(story.getAcceptanceCriteria())
			.difficulty(story.getDifficulty())
			.parentIssueKey(Optional.ofNullable(story.getParentIssue())
				.map(Issue::getIssueKey)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}
}
