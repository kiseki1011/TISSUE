package com.tissue.api.notification.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.notification.application.service.command.NotificationCommandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceCode}/notifications")
public class NotificationQueryController {

	/**
	 * Todo
	 *  - 워크스페이스 멤버의 모든 알림 조회(페이징 API)
	 *    - 이미 읽은 알림, 읽지 않은 알림 필터링 가능
	 *    - 검색 지원
	 *    - 엔티티 타입(Sprint, Issue, Review...) 조회 지원
	 *  - 워크스페이스 멤버의 특정 알림 읽기 처리
	 *  - 워크스페이스 멤버의 모든 알림 읽기 처리
	 *  - MemberController에 알림 조회 API
	 *    - 워크스페이스 관련 작업 제외 X
	 *    - Invitation, 추후에 추가할 친구 추가, 메세지 등에 대한 알림
	 */

	private final NotificationCommandService notificationCommandService;

	// @LoginRequired
	// @RoleRequired(role = WorkspaceRole.VIEWER)
	// @GetMapping
	// public ApiResponse<List<NotificationDto>> getUnreadNotifications(
	// 	@PathVariable String code,
	// 	@CurrentWorkspaceMember Long currentWorkspaceMemberId
	// ) {
	// 	List<NotificationDto> notifications = notificationService.getUnreadNotifications(
	// 		currentWorkspaceMemberId, code);
	//
	// 	return ApiResponse.ok("Unread notifications retrieved successfully", notifications);
	// }
	//
	// @LoginRequired
	// @RoleRequired(role = WorkspaceRole.VIEWER)
	// @PatchMapping("/{notificationId}/read")
	// public ApiResponse<Void> markAsRead(
	// 	@PathVariable String code,
	// 	@PathVariable Long notificationId,
	// 	@CurrentWorkspaceMember Long currentWorkspaceMemberId
	// ) {
	// 	notificationService.markAsRead(notificationId, currentWorkspaceMemberId);
	//
	// 	return ApiResponse.ok("Notification marked as read");
	// }
	//
	// @LoginRequired
	// @RoleRequired(role = WorkspaceRole.VIEWER)
	// @PatchMapping("/read-all")
	// public ApiResponse<Void> markAllAsRead(
	// 	@PathVariable String code,
	// 	@CurrentWorkspaceMember Long currentWorkspaceMemberId
	// ) {
	// 	notificationService.markAllAsRead(currentWorkspaceMemberId, code);
	//
	// 	return ApiResponse.ok("All notifications marked as read");
	// }
}
