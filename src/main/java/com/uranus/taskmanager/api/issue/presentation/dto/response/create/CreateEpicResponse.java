package com.uranus.taskmanager.api.issue.presentation.dto.response.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Epic;

import lombok.Builder;

@Builder
public record CreateEpicResponse(
	Long issueId,
	String workspaceCode,
	Long reporterId,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	String businessGoal,
	LocalDate targetReleaseDate,
	LocalDate hardDeadLine,
	Long parentIssueId
) implements CreateIssueResponse {

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}

	public static CreateEpicResponse from(Epic epic) {
		return CreateEpicResponse.builder()
			.issueId(epic.getId())
			.workspaceCode(epic.getWorkspaceCode())
			.reporterId(epic.getCreatedBy())
			.title(epic.getTitle())
			.content(epic.getContent())
			.summary(epic.getSummary())
			.priority(epic.getPriority())
			.dueDate(epic.getDueDate())
			.businessGoal(epic.getBusinessGoal())
			.targetReleaseDate(epic.getTargetReleaseDate())
			.hardDeadLine(epic.getHardDeadLine())
			.parentIssueId(epic.getParentIssue().getId())
			.build();
	}
}
