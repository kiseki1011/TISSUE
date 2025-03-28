package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import lombok.Getter;

/**
 * Todo
 *  - IssueStatusUpdated -> IssueStatusChanged
 */
@Getter
public class IssueStatusUpdatedEvent extends IssueEvent {

	// 이슈 상태 변경 정보
	private final IssueStatus oldStatus;
	private final IssueStatus newStatus;

	// 부모 이슈 정보 (있는 경우)
	private final Long parentIssueId;
	private final String parentIssueKey;
	private final IssueType parentIssueType;

	// 이슈의 스토리 포인트 (Epic 스토리 포인트 계산용)
	private final Integer storyPoint;

	private IssueStatusUpdatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		IssueStatus oldStatus,
		IssueStatus newStatus,
		Long parentIssueId,
		String parentIssueKey,
		IssueType parentIssueType,
		Integer storyPoint
	) {
		super(
			NotificationType.ISSUE_STATUS_CHANGED,
			NotificationEntityType.ISSUE,
			issueId,
			issueKey,
			workspaceCode,
			issueType,
			triggeredByWorkspaceMemberId
		);
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.parentIssueId = parentIssueId;
		this.parentIssueKey = parentIssueKey;
		this.parentIssueType = parentIssueType;
		this.storyPoint = storyPoint;
	}

	public static IssueStatusUpdatedEvent createEvent(
		Issue issue,
		IssueStatus oldStatus,
		Long triggeredByWorkspaceMemberId
	) {
		Issue parentIssue = issue.hasParent() ? issue.getParentIssue() : null;

		return new IssueStatusUpdatedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId,
			oldStatus,
			issue.getStatus(),
			parentIssue != null ? parentIssue.getId() : null,
			parentIssue != null ? parentIssue.getIssueKey() : null,
			parentIssue != null ? parentIssue.getType() : null,
			issue.getStoryPoint()
		);
	}

	/**
	 * 이슈가 CLOSED 상태로 변경되었는지 확인한다
	 */
	public boolean isClosedNow() {
		return newStatus == IssueStatus.CLOSED;
	}
}
