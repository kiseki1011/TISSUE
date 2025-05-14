package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueParentRemovedEvent extends IssueEvent {

	// 제거된 부모 이슈 정보
	private final Long removedParentIssueId;
	private final String removedParentIssueKey;
	private final IssueType removedParentIssueType;

	// 자식 이슈의 스토리 포인트 (Epic 스토리 포인트 계산용)
	private final Integer storyPoint;

	private IssueParentRemovedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long actorMemberId,
		Long removedParentIssueId,
		String removedParentIssueKey,
		IssueType removedParentIssueType,
		Integer storyPoint
	) {
		super(
			NotificationType.ISSUE_PARENT_REMOVED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);
		this.removedParentIssueId = removedParentIssueId;
		this.removedParentIssueKey = removedParentIssueKey;
		this.removedParentIssueType = removedParentIssueType;
		this.storyPoint = storyPoint;
	}

	/**
	 * 자식 이슈와 제거된 부모 이슈 정보를 사용하여 이벤트 객체를 생성하는 팩토리 메서드
	 *
	 * @param childIssue 부모가 제거된 자식 이슈
	 * @param removedParentIssue 제거된 부모 이슈
	 * @param actorMemberId 이벤트를 발생시킨 워크스페이스 멤버 ID
	 * @return 새로운 IssueParentRemovedEvent 인스턴스
	 */
	public static IssueParentRemovedEvent createEvent(
		Issue childIssue,
		Issue removedParentIssue,
		Long actorMemberId
	) {
		return new IssueParentRemovedEvent(
			childIssue.getId(),
			childIssue.getIssueKey(),
			childIssue.getWorkspaceCode(),
			childIssue.getType(),
			actorMemberId,
			removedParentIssue.getId(),
			removedParentIssue.getIssueKey(),
			removedParentIssue.getType(),
			childIssue.getStoryPoint()
		);
	}

	/**
	 * 제거된 부모 이슈가 Epic인지 확인한다
	 */
	public boolean wasRemovedParentEpic() {
		return removedParentIssueType == IssueType.EPIC;
	}
}
