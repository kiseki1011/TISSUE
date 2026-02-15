package com.tissue.notification.web;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.service.NotificationQueryService;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @PathVariable String workspaceKey,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentMember MemberDetails memberDetails) {

        CursorPageResponse<NotificationResponse> notifications =
                queryService.getNotifications(workspaceKey, memberDetails.getMemberId(), unreadOnly, cursorId, limit);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-status")
    public ResponseEntity<Boolean> checkUnreadStatus(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        boolean hasUnread = queryService.checkUnreadStatus(workspaceKey, memberDetails.getMemberId());
        return ResponseEntity.ok(hasUnread);
    }
}
