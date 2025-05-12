package com.tissue.api.comment.domain.event;

import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.model.enums.NotificationType;
import com.tissue.api.notification.domain.model.enums.ResourceType;

import lombok.Getter;

@Getter
public abstract class CommentEvent extends IssueEvent {

	private final Long commentId;

	protected CommentEvent(
		NotificationType notificationType,
		ResourceType resourceType,
		String workspaceCode,
		Long issueId,
		String issueKey,
		IssueType issueType,
		Long actorMemberId,
		Long commentId
	) {
		super(
			notificationType,
			resourceType,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);

		this.commentId = commentId;
	}
}
