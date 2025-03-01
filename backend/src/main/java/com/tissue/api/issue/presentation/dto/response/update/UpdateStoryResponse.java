package com.tissue.api.issue.presentation.dto.response.update;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;

import lombok.Builder;

@Builder
public record UpdateStoryResponse(
	Long issueId,
	String issueKey,
	String workspaceCode,

	Long updaterId,
	LocalDateTime updatedAt,

	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,

	String userStory,
	String acceptanceCriteria

) implements UpdateIssueResponse {

	public static UpdateStoryResponse from(Story story) {
		return UpdateStoryResponse.builder()
			.issueId(story.getId())
			.issueKey(story.getIssueKey())
			.workspaceCode(story.getWorkspaceCode())
			.updaterId(story.getLastModifiedByWorkspaceMember())
			.updatedAt(story.getLastModifiedDate())
			.title(story.getTitle())
			.content(story.getContent())
			.summary(story.getSummary())
			.priority(story.getPriority())
			.dueDate(story.getDueDate())
			.userStory(story.getUserStory())
			.acceptanceCriteria(story.getAcceptanceCriteria())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}
}
