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

	// TODO: entityId 대신 entityKey?
	// TODO: 추가 정보를 보관하기 위해 Map 사용? vs 추가 엔티티 정보를 보관할 DTO 만들기?
	//  - 예를 들어서 issueKey 외에도 reviewId 또는 commentId를 전달해야 하는 경우 어떻게 처리?

	@Transactional
	public void createNotification(
		UUID eventId,
		String workspaceCode,
		NotificationType type,
		NotificationEntityType entityType,
		Long entityId,
		Long actorWorkspaceMemberId,
		Long receiverWorkspaceMemberId,
		String title,
		String message
	) {
		WorkspaceMember actor = workspaceMemberReader.findWorkspaceMember(actorWorkspaceMemberId);

		Notification notification = Notification.builder()
			.eventId(eventId)
			.workspaceCode(workspaceCode)
			.type(type)
			.entityType(entityType)
			.entityId(entityId)
			.actorWorkspaceMemberId(actorWorkspaceMemberId)
			.actorWorkspaceMemberNickname(actor.getNickname())
			.receiverWorkspaceMemberId(receiverWorkspaceMemberId)
			.title(title)
			.message(message)
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
