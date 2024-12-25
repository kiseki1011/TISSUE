package com.tissue.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
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
	Long reporterId, // Todo: workspaceMemberDetail 사용 고려, SessionAuditorAware에서 workspaceMemberId 반환하는 형태로 변경
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
	Long parentIssueId
) implements CreateIssueResponse {

	public static CreateBugResponse from(Bug bug) {
		return CreateBugResponse.builder()
			.issueId(bug.getId())
			.issueKey(bug.getIssueKey())
			.workspaceCode(bug.getWorkspaceCode())
			.reporterId(bug.getCreatedBy())
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
			.parentIssueId(Optional.ofNullable(bug.getParentIssue())
				.map(Issue::getId)
				.orElse(null))
			.build();
	}

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}
}
