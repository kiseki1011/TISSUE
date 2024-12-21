package com.uranus.taskmanager.api.issue.presentation.dto.response.create;

import java.time.LocalDate;
import java.util.Set;

import com.uranus.taskmanager.api.issue.domain.enums.BugSeverity;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueStatus;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Bug;

import lombok.Builder;

@Builder
public record CreateBugResponse(
	Long issueId,
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

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}

	public static CreateBugResponse from(Bug bug) {
		return CreateBugResponse.builder()
			.issueId(bug.getId())
			.workspaceCode(bug.getWorkspaceCode())
			.reporterId(bug.getReporter())
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
			.parentIssueId(bug.getParentIssue().getId())
			.build();
	}
}
