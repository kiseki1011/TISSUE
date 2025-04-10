package com.tissue.api.comment.domain.event;

import com.tissue.api.comment.domain.IssueComment;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;

import lombok.Getter;

@Getter
public class IssueCommentAddedEvent extends CommentEvent {

	public IssueCommentAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		Long commentId
	) {
		super(
			NotificationType.ISSUE_COMMENT_ADDED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			triggeredByWorkspaceMemberId,
			commentId
		);
	}

	public static IssueCommentAddedEvent createEvent(
		Issue issue,
		IssueComment comment,
		Long triggeredByWorkspaceMemberId
	) {
		return new IssueCommentAddedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId,
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
