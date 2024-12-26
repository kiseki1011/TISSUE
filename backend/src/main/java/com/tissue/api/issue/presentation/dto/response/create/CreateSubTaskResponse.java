package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;

import lombok.Builder;

@Builder
public record CreateSubTaskResponse(
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

	public static CreateSubTaskResponse from(SubTask subTask) {
		return CreateSubTaskResponse.builder()
			.issueId(subTask.getId())
			.issueKey(subTask.getIssueKey())
			.workspaceCode(subTask.getWorkspaceCode())
			.reporterId(subTask.getCreatedByWorkspaceMember())
			.title(subTask.getTitle())
			.content(subTask.getContent())
			.summary(subTask.getSummary())
			.priority(subTask.getPriority())
			.dueDate(subTask.getDueDate())
			.difficulty(subTask.getDifficulty())
			.parentIssueId(Optional.ofNullable(subTask.getParentIssue())
				.map(Issue::getId)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}
}
