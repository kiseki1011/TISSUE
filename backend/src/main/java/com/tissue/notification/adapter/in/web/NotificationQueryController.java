package com.tissue.notification.adapter.in.web;

import com.tissue.common.dto.CursorPageResponse;
import com.tissue.notification.application.dto.response.NotificationResponse;
import com.tissue.notification.application.service.NotificationQueryService;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
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
            @CurrentMember MemberDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int limit) {

        CursorPageResponse<NotificationResponse> notifications =
                queryService.getNotifications(workspaceKey, userDetails.getMemberId(), unreadOnly, cursorId, limit);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-status")
    public ResponseEntity<Boolean> checkUnreadStatus(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails userDetails) {

        boolean hasUnread = queryService.checkUnreadStatus(workspaceKey, userDetails.getMemberId());
        return ResponseEntity.ok(hasUnread);
    }
}
