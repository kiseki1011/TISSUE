package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueCreatedEvent extends IssueEvent {

	public IssueCreatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long triggeredByWorkspaceMemberId
	) {
		super(
			NotificationType.ISSUE_CREATED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			triggeredByWorkspaceMemberId
		);
	}

	public static IssueCreatedEvent createEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId
	) {
		return new IssueCreatedEvent(
			issue.getId(),
			issue.getKey(),
			issue.getWorkspaceKey(),
			// issue.getType(),
			triggeredByWorkspaceMemberId
		);
	}
}
