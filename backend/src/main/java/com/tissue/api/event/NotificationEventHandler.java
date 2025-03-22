package com.tissue.api.event;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.service.command.NotificationCommandService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

	private final NotificationCommandService notificationService;
	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * 이슈 생성 이벤트 처리 - 워크스페이스 전체 멤버에게 알림
	 */
	@Async("notificationTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueCreated(IssueCreatedEvent event) {
		log.debug("Processing issue created event: {}", event.getIssue().getIssueKey());

		// Todo
		//  - Issue 엔티티를 넘겨서 사용하는게 아니라 id만 받아와서 조회하고 사용
		//  - 또는 전용 DTO 만들어서 데이터 전달
		Issue issue = event.getIssue();
		String workspaceCode = issue.getWorkspaceCode();
		Long actorId = event.getTriggeredByWorkspaceMemberId();

		// 액터 정보 조회
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(actorId);

		// 제목과 메시지 하드코딩
		// Todo
		//  - NotificationMessageFactory 만들어서 사용
		String title = "Created new issue: " + issue.getTitle();
		String message = actor.getNickname() + " has created issue " + issue.getIssueKey();

		// 워크스페이스의 모든 멤버 조회
		List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode);
		int totalMembers = members.size();
		int successCount = 0;
		int failCount = 0;

		// 각 멤버에게 알림 생성
		/*
		 * Todo
		 *  - AsIs: best-effort 방식 사용중. 일부 알림이 실패해도 재처리 로직 없이, 실패한 경우를 로깅정도만 함.
		 *  - ToBe: Transactional Outbox Pattern 사용
		 */
		for (WorkspaceMember member : members) {
			try {
				notificationService.createNotification(
					event.getEventId(),
					member.getId(),
					workspaceCode,
					NotificationType.ISSUE_CREATED,
					NotificationEntityType.ISSUE,
					issue.getId(),
					title,
					message,
					actorId
				);
				successCount++;
			} catch (ResourceNotFoundException e) {
				// 특정 리소스(예: 워크스페이스 멤버)를 찾을 수 없는 경우
				failCount++;
				log.warn("Resource not found while creating notification for member: {}", member.getId(), e);
			} catch (DataIntegrityViolationException e) {
				// 데이터 무결성 제약 조건 위반(예: 중복 알림)
				failCount++;
				log.warn("Data integrity violation while creating notification for member: {}", member.getId(), e);
			} catch (RuntimeException e) {
				// 그 외 런타임 예외
				// 중요한 오류는 모니터링을 위해 별도 처리
				failCount++;
				log.error("Failed to create notification for member: {}, message: {}",
					member.getId(), e.getMessage(), e);
			}
		}

		log.info("Notification creation summary - total: {}, success: {}, fail: {}, event: {}",
			totalMembers, successCount, failCount, event.getEventId());
	}
}
