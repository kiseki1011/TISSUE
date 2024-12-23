package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Story;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.validation.constraints.NotBlank;

public record CreateStoryRequest(
	@NotBlank String title,
	@NotBlank String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	Long parentIssueId,
	String userStory,
	String acceptanceCriteria

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.STORY;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Story.builder()
			.workspace(workspace)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueDate(dueDate)
			.difficulty(difficulty)
			.userStory(userStory)
			.acceptanceCriteria(acceptanceCriteria)
			.parentIssue(parentIssue)
			.build();
	}
}
