package com.tissue.feature.notification.web;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationQueryService;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.CursorPageResponse;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications")
public class NotificationManagementController {

    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;

    @Operation(
            summary = "Mark notification as read",
            description = "Mark a single notification of the current user as read.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notification marked as read"),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    @PostMapping("/{notificationId}:read")
    public ResponseEntity<Void> readNotification(
            @PathVariable Long notificationId, @CurrentMember MemberDetails currentMember) {
        commandService.readNotification(notificationId, currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Mark all of the current user's notifications in the workspace as read.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "All notifications marked as read"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @PostMapping(":readAll")
    public ResponseEntity<Void> readAllNotifications(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails currentMember) {
        commandService.readAllNotifications(workspaceKey, currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List notifications",
            description = "Retrieve the current user's notifications with cursor-based pagination."
                    + " Optionally filter by unread status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationResponse>> getNotifications(
            @PathVariable String workspaceKey,
            @Parameter(description = "Filter by unread notifications only")
                    @RequestParam(required = false, defaultValue = "false")
                    boolean unreadOnly,
            @Parameter(description = "ID of the last item from the previous page. Leave empty for the first page.")
                    @RequestParam(required = false)
                    Long cursorId,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20")
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        CursorPageResponse<NotificationResponse> notifications =
                queryService.getNotifications(workspaceKey, memberDetails.getMemberId(), unreadOnly, cursorId, limit);

        return ResponseEntity.ok(notifications);
    }

    @Operation(
            summary = "Check unread status",
            description = "Check whether the current user has any unread notifications.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unread status returned"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping("/unread-status")
    public ResponseEntity<Boolean> checkUnreadStatus(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        boolean hasUnread = queryService.checkUnreadStatus(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(hasUnread);
    }
}
