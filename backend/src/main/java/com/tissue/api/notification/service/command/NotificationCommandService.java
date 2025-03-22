package com.tissue.api.notification.service.command;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.notification.domain.Notification;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.repository.NotificationRepository;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

	private final NotificationRepository notificationRepository;
	private final WorkspaceMemberReader workspaceMemberReader;

	@Transactional
	public void createNotification(
		UUID eventId,
		Long receiverWorkspaceMemberId,
		String workspaceCode,
		NotificationType type,
		NotificationEntityType entityType,
		Long entityId,
		String title,
		String message,
		Long actorWorkspaceMemberId
	) {
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(actorWorkspaceMemberId);

		Notification notification = Notification.builder()
			.eventId(eventId)
			.receiverWorkspaceMemberId(receiverWorkspaceMemberId)
			.workspaceCode(workspaceCode)
			.type(type)
			.entityType(entityType)
			.entityId(entityId)
			.title(title)
			.message(message)
			.actorWorkspaceMemberId(actorWorkspaceMemberId)
			.actorWorkspaceMemberNickname(actor.getNickname())
			.build();

		notificationRepository.save(notification);
	}

	@Transactional
	public void markAsRead(Long notificationId, Long workspaceMemberId) {
		Notification notification = notificationRepository.findByIdAndReceiverWorkspaceMemberId(
				notificationId,
				workspaceMemberId
			)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Notification not found. notification id: %d", notificationId)));

		notification.markAsRead();
	}

	@Transactional
	public void markAllAsRead(Long workspaceMemberId, String workspaceCode) {
		List<Notification> notifications = notificationRepository
			.findByReceiverWorkspaceMemberIdAndWorkspaceCodeAndIsReadFalse(workspaceMemberId, workspaceCode);

		notifications.forEach(Notification::markAsRead);
		notificationRepository.saveAll(notifications);
	}
}
