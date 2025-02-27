package com.tissue.api.issue.presentation.dto.response.update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;

import lombok.Builder;

@Builder
public record UpdateBugResponse(
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
	Difficulty difficulty,

	String reproducingSteps,
	BugSeverity severity,
	Set<String> affectedVersions,
	IssueStatus status

) implements UpdateIssueResponse {

	public static UpdateBugResponse from(Bug bug) {
		return UpdateBugResponse.builder()
			.issueId(bug.getId())
			.issueKey(bug.getIssueKey())
			.workspaceCode(bug.getWorkspaceCode())
			.updaterId(bug.getLastModifiedByWorkspaceMember())
			.updatedAt(bug.getLastModifiedDate())
			.title(bug.getTitle())
			.content(bug.getContent())
			.summary(bug.getSummary())
			.priority(bug.getPriority())
			.dueDate(bug.getDueDate())
			.reproducingSteps(bug.getReproducingSteps())
			.severity(bug.getSeverity())
			.affectedVersions(bug.getAffectedVersions())
			.difficulty(bug.getDifficulty())
			.status(bug.getStatus())
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}
}
