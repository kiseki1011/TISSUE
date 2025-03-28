package com.tissue.api.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.notification.domain.NotificationProcessor;
import com.tissue.api.notification.domain.NotificationTargetResolver;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
		log.debug("Processing issue created event. issue id: {}", event.getIssueId());

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUpdated(IssueUpdatedEvent event) {
		log.debug("Processing issue updated event. issue id: {}", event.getIssueId());

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(event.getIssueId());
		notificationProcessor.processNotification(event, targets);
	}

}
