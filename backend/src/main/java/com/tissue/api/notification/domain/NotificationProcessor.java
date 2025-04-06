package com.tissue.api.notification.domain;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.notification.service.command.NotificationCommandService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {

	private final NotificationCommandService notificationService;
	private final NotificationMessageFactory notificationMessageFactory;

	/**
	 * 알림 처리 메서드 - 외부에서 알림 대상 주입
	 */
	public <T extends DomainEvent> void processNotification(
		T event,
		List<WorkspaceMember> targets
	) {
		NotificationMessage message = notificationMessageFactory.createMessage(event);

		/*
		 * Todo: 알림 생성 로직 개선
		 *  - AsIs: best-effort 방식 사용중. 일부 알림이 실패해도 재처리 로직 없이, 실패한 경우를 로깅정도만 함.
		 *  - ToBe: 이벤트 저장소 패턴 또는 Transactional Outbox Pattern 사용
		 */
		int totalTargets = targets.size();
		int successCount = 0;
		int failCount = 0;

		for (WorkspaceMember target : targets) {
			try {
				notificationService.createNotification(
					event,
					target.getId(),
					message.title(),
					message.content()
				);
				successCount++;
			} catch (ResourceNotFoundException e) {
				failCount++;
				log.warn("Resource not found while creating notification for member: {}", target.getId(), e);
			} catch (DataIntegrityViolationException e) {
				failCount++;
				log.warn("Data integrity violation while creating notification for member: {}", target.getId(), e);
			} catch (RuntimeException e) {
				failCount++;
				log.error("Failed to create notification for member: {}, message: {}",
					target.getId(), e.getMessage(), e);
			}
		}

		log.info("Notification creation summary - type: {}, total: {}, success: {}, fail: {}, event: {}",
			event.getNotificationType(), totalTargets, successCount, failCount, event.getEventId());
	}
}
