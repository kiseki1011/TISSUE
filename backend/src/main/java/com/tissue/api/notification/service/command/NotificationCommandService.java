package com.tissue.api.notification.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.notification.domain.Notification;
import com.tissue.api.notification.domain.repository.NotificationRepository;
import com.tissue.api.notification.domain.vo.EntityReference;
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
		DomainEvent event,
		Long receiverWorkspaceMemberId,
		String title,
		String message
	) {
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(event.getTriggeredByWorkspaceMemberId());

		EntityReference entityReference = event.createEntityReference();

		Notification notification = Notification.builder()
			.eventId(event.getEventId())
			.notificationType(event.getNotificationType())
			.entityReference(entityReference)
			.actorWorkspaceMemberId(event.getTriggeredByWorkspaceMemberId())
			.actorWorkspaceMemberNickname(actor.getNickname())
			.title(title)
			.message(message)
			.receiverWorkspaceMemberId(receiverWorkspaceMemberId)
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
			.findByReceiverWorkspaceMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(workspaceMemberId,
				workspaceCode);

		notifications.forEach(Notification::markAsRead);
		notificationRepository.saveAll(notifications);
	}
}
