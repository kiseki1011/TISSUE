package com.tissue.api.notification.service.query;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

	// @Transactional(readOnly = true)
	// public List<NotificationDto> getUnreadNotifications(Long workspaceMemberId, String workspaceCode) {
	// 	return notificationRepository.findByReceiverWorkspaceMemberIdAndWorkspaceCodeAndIsReadFalse(
	// 			workspaceMemberId, workspaceCode)
	// 		.stream()
	// 		.map(NotificationDto::from)
	// 		.collect(Collectors.toList());
	// }
}
