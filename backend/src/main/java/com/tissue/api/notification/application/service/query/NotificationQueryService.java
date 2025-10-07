package com.tissue.api.notification.application.service.query;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

	// @Transactional(readOnly = true)
	// public List<NotificationDto> getUnreadNotifications(Long workspaceMemberId, String workspaceKey) {
	// 	return notificationRepository.findByReceiverWorkspaceMemberIdAndWorkspaceCodeAndIsReadFalse(
	// 			workspaceMemberId, workspaceKey)
	// 		.stream()
	// 		.map(NotificationDto::from)
	// 		.collect(Collectors.toList());
	// }
}
