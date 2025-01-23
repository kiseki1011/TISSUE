package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;

import lombok.Builder;

@Builder
public record CreateBugResponse(

	Long issueId,
	String issueKey,
	String workspaceCode,
	Long createrId,
	LocalDateTime createdAt,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDate dueDate,
	String reproducingSteps,
	BugSeverity severity,
	Set<String> affectedVersions,
	Difficulty difficulty,
	IssueStatus status,
	String parentIssueKey

) implements CreateIssueResponse {

	public static CreateBugResponse from(Bug bug) {
		return CreateBugResponse.builder()
			.issueId(bug.getId())
			.issueKey(bug.getIssueKey())
			.workspaceCode(bug.getWorkspaceCode())
			.createrId(bug.getCreatedByWorkspaceMember())
			.createdAt(bug.getCreatedDate())
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
			.parentIssueKey(Optional.ofNullable(bug.getParentIssue())
				.map(Issue::getIssueKey)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}
}
