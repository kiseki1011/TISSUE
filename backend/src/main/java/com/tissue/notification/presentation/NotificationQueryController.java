package com.tissue.notification.presentation;

import com.tissue.notification.application.service.NotificationQueryService;
import com.tissue.notification.presentation.dto.response.NotificationResponse;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
import java.util.List;
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
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable String workspaceKey,
            @CurrentMember MemberDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {

        List<NotificationResponse> notifications =
                queryService.getNotifications(workspaceKey, userDetails.getMemberId(), unreadOnly);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unreadStatus")
    public ResponseEntity<Boolean> checkUnreadStatus(
            @PathVariable String workspaceKey,
            @CurrentMember MemberDetails userDetails) {

        boolean hasUnread = queryService.checkUnreadStatus(workspaceKey, userDetails.getMemberId());
        return ResponseEntity.ok(hasUnread);
    }
}
