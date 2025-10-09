package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueParentAssignedEvent extends IssueEvent {

	// 부모 이슈 정보
	private final Long parentIssueId;
	private final String parentIssueKey;
	// private final IssueType parentIssueType;

	// 이전 부모 이슈 정보 (있는 경우)
	private final Long oldParentIssueId;
	private final String oldParentIssueKey;
	// private final IssueType oldParentIssueType;

	// // 이슈의 현재 스토리 포인트 (Epic 스토리 포인트 계산용)
	// private final Integer storyPoint;

	private IssueParentAssignedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long actorMemberId,
		Long parentIssueId,
		String parentIssueKey,
		// IssueType parentIssueType,
		Long oldParentIssueId,
		String oldParentIssueKey
		// IssueType oldParentIssueType
	) {
		super(
			NotificationType.ISSUE_PARENT_ASSIGNED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			actorMemberId
		);
		this.parentIssueId = parentIssueId;
		this.parentIssueKey = parentIssueKey;
		// this.parentIssueType = parentIssueType;
		this.oldParentIssueId = oldParentIssueId;
		this.oldParentIssueKey = oldParentIssueKey;
		// this.oldParentIssueType = oldParentIssueType;
	}

	/**
	 * 자식 이슈, 새 부모 이슈, 이전 부모 이슈 정보를 사용하여 이벤트 객체를 생성하는 팩토리 메서드
	 *
	 * @param childIssue 부모가 할당된 자식 이슈
	 * @param parentIssue 새로 할당된 부모 이슈
	 * @param oldParentIssue 이전 부모 이슈 (없으면 null)
	 * @param actorMemberId 이벤트를 발생시킨 워크스페이스 멤버 ID
	 * @return 새로운 IssueParentAssignedEvent 인스턴스
	 */
	public static IssueParentAssignedEvent createEvent(
		Issue childIssue,
		Issue parentIssue,
		Issue oldParentIssue,
		Long actorMemberId
	) {
		return new IssueParentAssignedEvent(
			childIssue.getId(),
			childIssue.getKey(),
			childIssue.getWorkspaceKey(),
			// childIssue.getType(),
			actorMemberId,
			parentIssue.getId(),
			parentIssue.getKey(),
			// parentIssue.getType(),
			oldParentIssue != null ? oldParentIssue.getId() : null,
			oldParentIssue != null ? oldParentIssue.getKey() : null
			// oldParentIssue != null ? oldParentIssue.getType() : null,
			// childIssue.getStoryPoint()
		);
	}

	/**
	 * 새 부모 이슈가 Epic인지 확인한다
	 */
	// public boolean isNewParentEpic() {
	// 	return parentIssueType == IssueType.EPIC;
	// }

	/**
	 * 이전 부모 이슈가 Epic인지 확인한다
	 */
	// public boolean wasOldParentEpic() {
	// 	return oldParentIssueType == IssueType.EPIC;
	// }
}
