package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDateTime;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.SubTask;

import lombok.Builder;

@Builder
public record CreateSubTaskResponse(

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

	public static CreateSubTaskResponse from(SubTask subTask) {
		return CreateSubTaskResponse.builder()
			.issueId(subTask.getId())
			.issueKey(subTask.getIssueKey())
			.workspaceCode(subTask.getWorkspaceCode())
			.createrId(subTask.getCreatedBy())
			.createdAt(subTask.getCreatedDate())
			.title(subTask.getTitle())
			.content(subTask.getContent())
			.summary(subTask.getSummary())
			.priority(subTask.getPriority())
			.dueAt(subTask.getDueAt())
			.parentIssueKey(Optional.ofNullable(subTask.getParentIssue())
				.map(Issue::getIssueKey)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.SUB_TASK;
	}
}
