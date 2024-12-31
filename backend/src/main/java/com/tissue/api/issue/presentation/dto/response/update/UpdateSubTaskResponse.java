package com.tissue.api.issue.presentation.dto.response.update;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;

import lombok.Builder;

@Builder
public record UpdateSubTaskResponse(
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

	public static UpdateSubTaskResponse from(SubTask subTask) {
		return UpdateSubTaskResponse.builder()
			.issueId(subTask.getId())
			.issueKey(subTask.getIssueKey())
			.workspaceCode(subTask.getWorkspaceCode())
			.updaterId(subTask.getLastModifiedByWorkspaceMember())
			.updatedAt(subTask.getLastModifiedDate())
			.title(subTask.getTitle())
			.content(subTask.getContent())
			.summary(subTask.getSummary())
			.priority(subTask.getPriority())
			.dueDate(subTask.getDueDate())
			.difficulty(subTask.getDifficulty())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}
}
