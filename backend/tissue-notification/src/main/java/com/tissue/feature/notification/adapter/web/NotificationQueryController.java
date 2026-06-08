package com.tissue.feature.notification.adapter.web;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.usecase.NotificationQueryUseCase;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final NotificationQueryUseCase notificationQueryUseCase;

    @Operation(operationId = "listNotifications", summary = "List notifications", description = """
                    List the current user's notifications (newest first). \
                    Optional `unreadOnly` filter limits results to unread items.

                    **Pagination (cursor-based):**
                    - First page: omit `cursor`.
                    - Next page: pass the `nextCursor` from the previous response.
                    - `limit` controls page size (default 20).

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Notifications retrieved")})
    @GetMapping("/notifications")
    public ResponseEntity<CursorPage<NotificationResponse>> listNotifications(
            @Parameter(description = "Filter by unread notifications only")
                    @RequestParam(required = false, defaultValue = "false")
                    boolean unreadOnly,
            @Parameter(description = "Opaque cursor from the previous page's `nextCursor`. Omit for the first page.")
                    @RequestParam(required = false)
                    @Nullable
                    String cursor,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20")
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        CursorPage<NotificationResponse> notifications =
                notificationQueryUseCase.getNotifications(memberDetails.getMemberId(), unreadOnly, cursor, limit);

        return ResponseEntity.ok(notifications);
    }

    @Operation(operationId = "checkNotificationUnreadStatus", summary = "Check unread status", description = """
                    Check whether the current user has any unread notifications.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Unread status returned")})
    @GetMapping("/notifications/unread-status")
    public ResponseEntity<Boolean> checkNotificationUnreadStatus(@CurrentMember MemberDetails memberDetails) {
        boolean hasUnread = notificationQueryUseCase.checkUnreadStatus(memberDetails.getMemberId());

        return ResponseEntity.ok(hasUnread);
    }
}
