package com.tissue.api.comment.domain.event;

import com.tissue.api.comment.domain.model.IssueComment;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.model.vo.EntityReference;

import lombok.Getter;

@Getter
public class IssueCommentAddedEvent extends CommentEvent {

	public IssueCommentAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long actorMemberId,
		Long commentId
	) {
		super(
			NotificationType.ISSUE_COMMENT_ADDED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			actorMemberId,
			commentId
		);
	}

	public static IssueCommentAddedEvent createEvent(
		Issue issue,
		IssueComment comment,
		Long actorMemberId
	) {
		return new IssueCommentAddedEvent(
			issue.getId(),
			issue.getKey(),
			issue.getWorkspaceKey(),
			// issue.getType(),
			actorMemberId,
			comment.getId()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forIssueComment(
			getWorkspaceCode(),
			getIssueKey(),
			getCommentId()
		);
	}
}
