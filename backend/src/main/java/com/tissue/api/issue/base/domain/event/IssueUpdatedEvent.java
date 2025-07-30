package com.tissue.api.issue.base.domain.event;

import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueUpdatedEvent extends IssueEvent {

	// 변경된 이슈 제목 (로깅 및 알림 목적)
	private final String title;

	// 스토리 포인트 변경 여부 플래그
	// private final boolean storyPointChanged;

	// 변경 전 스토리 포인트 값
	// private final Integer oldStoryPoint;

	// 변경 후 스토리 포인트 값
	// private final Integer newStoryPoint;

	private IssueUpdatedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		String title
		// boolean storyPointChanged,
		// Integer oldStoryPoint,
		// Integer newStoryPoint
	) {
		super(
			NotificationType.ISSUE_UPDATED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			triggeredByWorkspaceMemberId
		);
		this.title = title;
		// this.storyPointChanged = storyPointChanged;
		// this.oldStoryPoint = oldStoryPoint;
		// this.newStoryPoint = newStoryPoint;
	}

	/**
	 * Issue 엔티티와 추가 정보를 사용하여 이벤트 객체를 생성하는 팩토리 메서드
	 *
	 * @param issue 업데이트된 이슈 엔티티
	// * @param oldStoryPoint 변경 전 스토리 포인트 값
	 * @param triggeredByWorkspaceMemberId 이벤트를 발생시킨 워크스페이스 멤버 ID
	 * @return 새로운 IssueContentUpdatedEvent 인스턴스
	 */
	public static IssueUpdatedEvent createEvent(
		Issue issue,
		// Integer oldStoryPoint,
		Long triggeredByWorkspaceMemberId
	) {
		return new IssueUpdatedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			// issue.getType(),
			triggeredByWorkspaceMemberId,
			issue.getTitle()
			// !Objects.equals(oldStoryPoint, issue.getStoryPoint()),
			// oldStoryPoint,
			// issue.getStoryPoint()
		);
	}
}
