package com.tissue.api.issue.presentation.dto.response.update;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Task;

import lombok.Builder;

@Builder
public record UpdateTaskResponse(
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
	Difficulty difficulty
) implements UpdateIssueResponse {

	public static UpdateTaskResponse from(Task task) {
		return UpdateTaskResponse.builder()
			.issueId(task.getId())
			.issueKey(task.getIssueKey())
			.workspaceCode(task.getWorkspaceCode())
			.updaterId(task.getLastModifiedByWorkspaceMember())
			.updatedAt(task.getLastModifiedDate())
			.title(task.getTitle())
			.content(task.getContent())
			.summary(task.getSummary())
			.priority(task.getPriority())
			.dueDate(task.getDueDate())
			.difficulty(task.getDifficulty())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}
}
