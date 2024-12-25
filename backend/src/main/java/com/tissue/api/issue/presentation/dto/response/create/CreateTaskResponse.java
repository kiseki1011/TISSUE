package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Task;

import lombok.Builder;

@Builder
public record CreateTaskResponse(
	Long issueId,
	String issueKey,
	String workspaceCode,
	Long reporterId,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	Difficulty difficulty,
	Long parentIssueId
) implements CreateIssueResponse {

	public static CreateTaskResponse from(Task task) {
		return CreateTaskResponse.builder()
			.issueId(task.getId())
			.issueKey(task.getIssueKey())
			.workspaceCode(task.getWorkspaceCode())
			.reporterId(task.getCreatedBy())
			.title(task.getTitle())
			.content(task.getContent())
			.summary(task.getSummary())
			.priority(task.getPriority())
			.dueDate(task.getDueDate())
			.difficulty(task.getDifficulty())
			.parentIssueId(Optional.ofNullable(task.getParentIssue())
				.map(Issue::getId)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}
}
