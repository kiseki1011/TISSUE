package com.tissue.api.issue.application.dto.response.info;

import java.time.Instant;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.Builder;

// TODO: 추후에 정말 필요한 필드만 사용하도록 리팩토링 하거나 새로운 응답 DTO 추가
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
	public static IssueBasicInfo from(Issue issue, WorkspaceMember author, WorkspaceMember lastUpdatedBy) {
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
