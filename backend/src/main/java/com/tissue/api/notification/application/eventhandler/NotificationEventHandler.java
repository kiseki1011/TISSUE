package com.tissue.api.notification.application.eventhandler;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tissue.api.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.api.comment.domain.event.ReviewCommentAddedEvent;
import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.event.IssueAssignedEvent;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.api.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueUnassignedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.notification.application.service.command.NotificationCommandService;
import com.tissue.api.notification.application.service.command.NotificationProcessor;
import com.tissue.api.notification.application.service.command.NotificationTargetService;
import com.tissue.api.notification.domain.model.ActivityLog;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;
import com.tissue.api.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.api.notification.infrastructure.repository.ActivityLogRepository;
import com.tissue.api.review.domain.event.ReviewSubmittedEvent;
import com.tissue.api.sprint.domain.event.SprintCompletedEvent;
import com.tissue.api.sprint.domain.event.SprintStartedEvent;
import com.tissue.api.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

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

	private final NotificationCommandService commandService;
	private final NotificationProcessor notificationProcessor;
	private final NotificationTargetService targetResolver;
	private final NotificationMessageFactory notificationMessageFactory;
	private final ActivityLogRepository activityLogRepository;

	/**
	 * 이슈 생성 이벤트 처리 - 워크스페이스 전체 멤버에게 알림
	 */
	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueCreated(IssueCreatedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUpdated(IssueUpdatedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueAssigned(IssueAssignedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getSpecificMemberTarget(
			event.getWorkspaceCode(),
			event.getAssignedMemberId()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUnassigned(IssueUnassignedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getSpecificMemberTarget(
			event.getWorkspaceCode(),
			event.getAssigneeMemberId()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueStatusChanged(IssueStatusChangedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentAssigned(IssueParentAssignedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentRemoved(IssueParentRemovedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewerAdded(IssueReviewerAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewRequested(IssueReviewRequestedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewSubmitted(ReviewSubmittedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueCommentCreated(IssueCommentAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewCommentCreated(ReviewCommentAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSprintStarted(SprintStartedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSprintCompleted(SprintCompletedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());
		processNotifications(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleWorkspaceMemberRoleChanged(WorkspaceMemberRoleChangedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getAdminAndSpecificMemberTargets(
			event.getWorkspaceCode(),
			event.getTargetMemberId()
		);
		processNotifications(event, targets);
	}

	private <T extends DomainEvent> void processNotifications(T event, Collection<WorkspaceMember> targets) {

		NotificationMessage message = notificationMessageFactory.createMessage(event);
		activityLogRepository.save(ActivityLog.from(event, message));

		if (targets == null || targets.isEmpty()) {
			log.debug("No notification targets for event: {}", event.getEventId());
			return;
		}

		for (WorkspaceMember target : targets) {
			Notification notification = commandService.createNotification(event, target.getMember().getId(), message);
			notificationProcessor.process(notification);
		}
	}
}
