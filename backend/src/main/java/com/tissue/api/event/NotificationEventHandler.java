package com.tissue.api.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.notification.domain.NotificationProcessor;
import com.tissue.api.notification.domain.NotificationTargetResolver;
import com.tissue.api.review.domain.event.ReviewRequestedEvent;
import com.tissue.api.review.domain.event.ReviewSubmittedEvent;
import com.tissue.api.review.domain.event.ReviewerAddedEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Todo: 알림 대상 정하기 -> 알림 처리를 하는 일련의 로직이 계속 중복된다. 추상화를 해서 중복 로직 제거?
 *  - 방법1: IssueEvent를 한번에 처리(IssueCreatedEvent를 중복 처리하지 않을 방법 찾아야 함)
 *  - 방법2: 전략 패턴 사용
 *    - 케이스 별로 로깅하는 방법은 이벤트에서 NotificationType을 꺼내서 사용
 *    - 실제로 달라지는 전략은 이벤트 별로 적용되는 알림 대상들(targets)
 *  - 방법3: 그냥 지금과 같은 방법 유지. 어차피 NotificationType은 많아봤자 10개를 넘지 않을 예정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

	private final NotificationProcessor notificationProcessor;
	private final NotificationTargetResolver targetResolver;

	/**
	 * 이슈 생성 이벤트 처리 - 워크스페이스 전체 멤버에게 알림
	 */
	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueCreated(IssueCreatedEvent event) {
		log.debug(
			"Processing issue created event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUpdated(IssueUpdatedEvent event) {
		log.debug(
			"Processing issue updated event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueStatusChanged(IssueStatusChangedEvent event) {
		log.debug(
			"Processing issue status changed event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentAssigned(IssueParentAssignedEvent event) {
		log.debug(
			"Processing issue parent assigned event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentRemoved(IssueParentRemovedEvent event) {
		log.debug(
			"Processing issue parent removed event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewerAdded(ReviewerAddedEvent event) {
		log.debug(
			"Processing issue reviewer added event. issue key: {}, workspace code: {}, reviewer wm id: {}",
			event.getIssueKey(), event.getWorkspaceCode(), event.getReviewerWorkspaceMemberId()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewRequested(ReviewRequestedEvent event) {
		log.debug(
			"Processing issue review requested event. issue key: {}, workspace code: {}",
			event.getIssueKey(), event.getWorkspaceCode()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewSubmitted(ReviewSubmittedEvent event) {
		log.debug(
			"Processing issue review submitted event. issue key: {}, workspace code: {}, reviewId: {}",
			event.getIssueKey(), event.getWorkspaceCode(), event.getReviewId()
		);

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}
}
