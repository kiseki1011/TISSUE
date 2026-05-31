package com.tissue.feature.notification.web;

import com.tissue.feature.notification.application.port.usecase.NotificationCommandUseCase;
import com.tissue.feature.notification.domain.exception.NotificationErrorCode;
import com.tissue.global.openapi.NotificationErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationCommandController {

    private final NotificationCommandUseCase notificationCommandUseCase;

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
            @PathVariable Long notificationId, @CurrentMember MemberDetails currentMember) {
        notificationCommandUseCase.readNotification(notificationId, currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "readAllNotifications",
            summary = "Mark all notifications as read",
            description = "Mark all of the current user's notifications as read.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "All notifications marked as read")})
    @PostMapping("/notifications:readAll")
    public ResponseEntity<Void> readAllNotifications(@CurrentMember MemberDetails currentMember) {
        notificationCommandUseCase.readAllNotifications(currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
