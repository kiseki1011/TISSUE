package com.tissue.notification.adapter.in.web;

import com.tissue.notification.application.service.NotificationCommandService;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications")
public class NotificationCommandController {

    private final NotificationCommandService commandService;

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable String workspaceKey,
            @PathVariable Long notificationId,
            @CurrentMember MemberDetails userDetails) {

        commandService.readNotification(notificationId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails userDetails) {

        commandService.readAllNotifications(workspaceKey, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
