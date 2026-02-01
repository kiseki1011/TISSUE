package com.tissue.notification.adapter.web;

import com.tissue.global.security.principal.MemberDetails;
import com.tissue.notification.application.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        @PathVariable Long notificationId, @AuthenticationPrincipal MemberDetails currentMember) {

        commandService.readNotification(notificationId, currentMember.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications(
        @PathVariable String workspaceKey, @AuthenticationPrincipal MemberDetails currentMember) {

        commandService.readAllNotifications(workspaceKey, currentMember.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
