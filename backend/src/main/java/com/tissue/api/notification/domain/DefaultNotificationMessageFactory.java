package com.tissue.api.notification.domain;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultNotificationMessageFactory implements NotificationMessageFactory {

	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueReader issueReader;

	@Override
	public NotificationMessage createMessage(
		NotificationType notificationType,
		NotificationEntityType entityType,
		Long entityId,
		String entityKey,
		Long triggeredByWorkspaceMemberId,
		String workspaceCode
	) {
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(triggeredByWorkspaceMemberId);
		String actorNickname = actor.getNickname();

		// 다른 알림 타입에 대한 처리
		return switch (notificationType) {
			case ISSUE_CREATED -> {
				Issue issue = issueReader.findIssue(entityKey, workspaceCode);
				yield new NotificationMessage(
					"Created new issue: " + issue.getTitle(),
					actorNickname + " has created issue " + entityKey
				);
			}
			case ISSUE_UPDATED -> {
				Issue issue = issueReader.findIssue(entityKey, workspaceCode);
				yield new NotificationMessage(
					"Updated issue: " + issue.getTitle(),
					actorNickname + " has updated issue " + entityKey
				);
			}
			default -> new NotificationMessage(
				"Notification from " + workspaceCode,
				"You have a new notification"
			);
		};

	}
}