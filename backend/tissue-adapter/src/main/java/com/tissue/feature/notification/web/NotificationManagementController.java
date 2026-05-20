package com.tissue.feature.notification.web;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationQueryService;
import com.tissue.feature.notification.domain.exception.NotificationErrorCode;
import com.tissue.global.openapi.NotificationErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.KeysetPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class NotificationManagementController {

    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;

    @Operation(
            operationId = "readNotification",
            summary = "Mark notification as read",
            description = "Mark a single notification of the current user as read.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notification marked as read"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @NotificationErrors({
        NotificationErrorCode.NOTIFICATION_NOT_FOUND,
        NotificationErrorCode.NOT_YOUR_NOTIFICATION,
    })
    @PostMapping("/notifications/{notificationId}:read")
    public ResponseEntity<Void> readNotification(
            @PathVariable String workspaceKey,
            @PathVariable Long notificationId,
            @CurrentMember MemberDetails currentMember) {
        commandService.readNotification(notificationId, currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "readAllNotifications",
            summary = "Mark all notifications as read",
            description = "Mark all of the current user's notifications in the workspace as read.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "All notifications marked as read")})
    @PostMapping("/notifications:readAll")
    public ResponseEntity<Void> readAllNotifications(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails currentMember) {
        commandService.readAllNotifications(workspaceKey, currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "listNotifications", summary = "List notifications", description = """
                Retrieve the current user's notifications with keyset-based pagination.\
                 Optionally filter by unread status.""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Notifications retrieved")})
    @GetMapping("/notifications")
    public ResponseEntity<KeysetPageResponse<NotificationResponse>> listNotifications(
            @PathVariable String workspaceKey,
            @Parameter(description = "Filter by unread notifications only")
                    @RequestParam(required = false, defaultValue = "false")
                    boolean unreadOnly,
            @Parameter(description = "ID of the last item from the previous page. Leave empty for the first page.")
                    @RequestParam(required = false)
                    Long keysetId,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20")
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        KeysetPageResponse<NotificationResponse> notifications =
                queryService.getNotifications(workspaceKey, memberDetails.getMemberId(), unreadOnly, keysetId, limit);

        return ResponseEntity.ok(notifications);
    }

    @Operation(
            operationId = "checkNotificationUnreadStatus",
            summary = "Check unread status",
            description = "Check whether the current user has any unread notifications.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Unread status returned")})
    @GetMapping("/notifications/unread-status")
    public ResponseEntity<Boolean> checkNotificationUnreadStatus(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        boolean hasUnread = queryService.checkUnreadStatus(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(hasUnread);
    }
}
