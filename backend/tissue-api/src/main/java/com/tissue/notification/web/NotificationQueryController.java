package com.tissue.notification.web;

import com.tissue.dto.CursorPageResponse;
import com.tissue.notification.application.dto.response.NotificationResponse;
import com.tissue.notification.application.service.NotificationQueryService;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications")
public class NotificationQueryController {

    private final NotificationQueryService queryService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationResponse>> getNotifications(
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int limit) {

        CursorPageResponse<NotificationResponse> notifications =
                queryService.getNotifications(currentWorkspaceMember, unreadOnly, cursorId, limit);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-status")
    public ResponseEntity<Boolean> checkUnreadStatus(
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        boolean hasUnread = queryService.checkUnreadStatus(currentWorkspaceMember);
        return ResponseEntity.ok(hasUnread);
    }
}
