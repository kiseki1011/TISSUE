package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDateTime;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Task;

import lombok.Builder;

@Builder
public record CreateTaskResponse(

	Long issueId,
	String issueKey,
	String workspaceCode,
	Long createrId,
	LocalDateTime createdAt,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDateTime dueAt,
	String parentIssueKey

) implements CreateIssueResponse {

	public static CreateTaskResponse from(Task task) {
		return CreateTaskResponse.builder()
			.issueId(task.getId())
			.issueKey(task.getIssueKey())
			.workspaceCode(task.getWorkspaceCode())
			.createrId(task.getCreatedByWorkspaceMember())
			.createdAt(task.getCreatedDate())
			.title(task.getTitle())
			.content(task.getContent())
			.summary(task.getSummary())
			.priority(task.getPriority())
			.dueAt(task.getDueAt())
			.parentIssueKey(Optional.ofNullable(task.getParentIssue())
				.map(Issue::getIssueKey)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.TASK;
	}
}
