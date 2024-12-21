package com.uranus.taskmanager.api.issue.presentation.dto.response.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Task;

import lombok.Builder;

@Builder
public record CreateTaskResponse(
	Long issueId,
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

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}

	public static CreateTaskResponse from(Task task) {
		return CreateTaskResponse.builder()
			.issueId(task.getId())
			.workspaceCode(task.getWorkspaceCode())
			.reporterId(task.getReporter())
			.title(task.getTitle())
			.content(task.getContent())
			.summary(task.getSummary())
			.priority(task.getPriority())
			.dueDate(task.getDueDate())
			.difficulty(task.getDifficulty())
			.parentIssueId(task.getParentIssue().getId())
			.build();
	}
}
