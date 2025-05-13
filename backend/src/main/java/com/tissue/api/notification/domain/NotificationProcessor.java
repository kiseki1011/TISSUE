package com.tissue.api.notification.domain;

import java.util.Collection;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.notification.application.service.command.NotificationCommandService;
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
	 * Handles notification processing for the given event and target members.
	 * Targets are injected externally.
	 * <p>
	 * This method uses a best-effort approach.
	 * Notifications are attempted for all targets, and failures are logged without retry logic.
	 */
	// TODO: Consider using Transactional Outbox Pattern
	public <T extends DomainEvent> void processNotification(
		T event,
		Collection<WorkspaceMember> targets
	) {
		NotificationMessage message = notificationMessageFactory.createMessage(event);

		int totalTargets = targets.size();
		int successCount = 0;
		int failCount = 0;

		for (WorkspaceMember target : targets) {
			try {
				notificationService.createNotification(event, target.getId(), message);
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
