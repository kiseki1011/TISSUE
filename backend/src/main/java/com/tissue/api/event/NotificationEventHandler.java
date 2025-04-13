package com.tissue.api.event;

import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tissue.api.assignee.domain.event.IssueAssignedEvent;
import com.tissue.api.assignee.domain.event.IssueUnassignedEvent;
import com.tissue.api.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.api.comment.domain.event.ReviewCommentAddedEvent;
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
import com.tissue.api.sprint.domain.event.SprintCompletedEvent;
import com.tissue.api.sprint.domain.event.SprintStartedEvent;
import com.tissue.api.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;

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

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUpdated(IssueUpdatedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	// TODO: workspaceCode + id 대신 id만 사용해서 WorkspaceMember 조회
	//  -> 굳이 workspaceCode를 같이 사용해서 WorkspaceMember를 조회해야 할까?
	//  -> id만 사용하는게 더 빠른데?
	//  -> workspace에 속하는 것은 이미 서비스 계층에서 보장해주는데?
	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueAssigned(IssueAssignedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getSpecificMember(event.getWorkspaceCode(),
			event.getAssignedWorkspaceMemberId());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueUnassigned(IssueUnassignedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getSpecificMember(event.getWorkspaceCode(),
			event.getAssigneeWorkspaceMemberId());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueStatusChanged(IssueStatusChangedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentAssigned(IssueParentAssignedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentRemoved(IssueParentRemovedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewerAdded(ReviewerAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewRequested(ReviewRequestedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewSubmitted(ReviewSubmittedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueCommentCreated(IssueCommentAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleReviewCommentCreated(ReviewCommentAddedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getIssueSubscriberTargets(
			event.getIssueKey(),
			event.getWorkspaceCode()
		);

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSprintStarted(SprintStartedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSprintCompleted(SprintCompletedEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {

		List<WorkspaceMember> targets = targetResolver.getWorkspaceWideMemberTargets(event.getWorkspaceCode());

		notificationProcessor.processNotification(event, targets);
	}

	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleWorkspaceMemberRoleChanged(WorkspaceMemberRoleChangedEvent event) {

		Set<WorkspaceMember> targets = targetResolver.getAdminsAndSpecificMember(
			event.getWorkspaceCode(),
			event.getTargetWorkspaceMemberId()
		);

		notificationProcessor.processNotification(event, targets);
	}
}
