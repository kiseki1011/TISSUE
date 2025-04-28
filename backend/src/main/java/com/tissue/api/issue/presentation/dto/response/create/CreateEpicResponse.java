package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;

import lombok.Builder;

@Builder
public record CreateEpicResponse(

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
	String businessGoal

) implements CreateIssueResponse {

	public static CreateEpicResponse from(Epic epic) {
		return CreateEpicResponse.builder()
			.issueId(epic.getId())
			.issueKey(epic.getIssueKey())
			.workspaceCode(epic.getWorkspaceCode())
			.createrId(epic.getCreatedBy())
			.createdAt(epic.getCreatedDate())
			.title(epic.getTitle())
			.content(epic.getContent())
			.summary(epic.getSummary())
			.priority(epic.getPriority())
			.dueAt(epic.getDueAt())
			.businessGoal(epic.getBusinessGoal())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}
}
