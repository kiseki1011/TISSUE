package com.tissue.api.notification.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.notification.infrastructure.repository.NotificationRepository;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

	private final NotificationRepository notificationRepository;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	// @Transactional
	// public Notification createNotification(
	// 	DomainEvent event,
	// 	Long receiverMemberId,
	// 	NotificationMessage message
	// ) {
	// 	WorkspaceMember actor = workspaceMemberFinder.findBy(
	// 		event.getActorMemberId(),
	// 		event.getWorkspaceCode()
	// 	);
	//
	// 	WorkspaceMember receiver = workspaceMemberFinder.findBy(
	// 		receiverMemberId,
	// 		event.getWorkspaceCode()
	// 	);
	//
	// 	EntityReference entityReference = event.createEntityReference();
	//
	// 	Notification notification = Notification.builder()
	// 		.eventId(event.getEventId())
	// 		.notificationType(event.getNotificationType())
	// 		.entityReference(entityReference)
	// 		.actorMemberId(event.getActorMemberId())
	// 		.actorDisplayName(actor.getDisplayName())
	// 		.message(message)
	// 		.receiverMemberId(receiverMemberId)
	// 		.receiverEmail(receiver.getEmail())
	// 		.build();
	//
	// 	return notificationRepository.save(notification);
	// }
	//
	// @Transactional
	// public void markAsRead(Long notificationId, Long receiverMemberId) {
	// 	Notification notification = notificationRepository.findByIdAndReceiverMemberId(
	// 			notificationId,
	// 			receiverMemberId
	// 		)
	// 		.orElseThrow(() -> new NotificationNotFoundException(notificationId));
	//
	// 	notification.markAsRead();
	// }
	//
	// @Transactional
	// public void markAllAsRead(Long memberId, String workspaceCode) {
	// 	List<Notification> notifications = notificationRepository
	// 		.findByReceiverMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(memberId,
	// 			workspaceCode);
	//
	// 	notifications.forEach(Notification::markAsRead);
	// 	notificationRepository.saveAll(notifications);
	// }
}
