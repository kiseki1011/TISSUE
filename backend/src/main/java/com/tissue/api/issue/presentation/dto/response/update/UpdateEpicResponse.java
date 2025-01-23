package com.tissue.api.issue.presentation.dto.response.update;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;

import lombok.Builder;

@Builder
public record UpdateEpicResponse(

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
	String businessGoal,
	LocalDate targetReleaseDate,
	LocalDate hardDeadLine

) implements UpdateIssueResponse {

	public static UpdateEpicResponse from(Epic epic) {
		return UpdateEpicResponse.builder()
			.issueId(epic.getId())
			.issueKey(epic.getIssueKey())
			.workspaceCode(epic.getWorkspaceCode())
			.updaterId(epic.getLastModifiedByWorkspaceMember())
			.updatedAt(epic.getLastModifiedDate())
			.title(epic.getTitle())
			.content(epic.getContent())
			.summary(epic.getSummary())
			.priority(epic.getPriority())
			.dueDate(epic.getDueDate())
			.businessGoal(epic.getBusinessGoal())
			.targetReleaseDate(epic.getTargetReleaseDate())
			.hardDeadLine(epic.getHardDeadLine())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.EPIC;
	}
}
