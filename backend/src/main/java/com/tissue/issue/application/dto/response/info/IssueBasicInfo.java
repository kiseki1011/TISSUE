package com.tissue.issue.application.dto.response.info;

import java.time.Instant;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.project.domain.ProjectMember;

import lombok.Builder;

@Builder
public record IssueBasicInfo(
	String issueKey,
	IssueTypeInfo issueType,
	String title,
	Instant createdAt,
	Instant lastUpdatedAt,
	ParticipantInfo author,
	ParticipantInfo lastUpdatedBy,
	ParticipantInfo assignee,
	IssuePriority priority,
	StateInfo currentState
) {
	public static IssueBasicInfo from(Issue issue, ProjectMember author, ProjectMember lastUpdatedBy) {
		return IssueBasicInfo.builder()
			.issueKey(issue.getKey())
			.issueType(IssueTypeInfo.from(issue.getIssueType()))
			.title(issue.getTitle())
			.createdAt(issue.getCreatedAt())
			.lastUpdatedAt(issue.getLastModifiedAt())
			.author(ParticipantInfo.from(author))
			.lastUpdatedBy(ParticipantInfo.from(lastUpdatedBy))
			.assignee(ParticipantInfo.from(issue.getParticipants().getAssignee()))
			.priority(issue.getPriority())
			.currentState(StateInfo.from(issue.getCurrentState()))
			.build();
	}
}
