package com.tissue.notification.application.service;

import com.tissue.dto.CursorPageResponse;
import com.tissue.notification.application.dto.response.NotificationResponse;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.domain.Notification;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public CursorPageResponse<NotificationResponse> getNotifications(
            WorkspaceMemberContext actorContext, boolean unreadOnly, @Nullable Long cursorId, int limit) {

        List<Notification> notifications;
        PageRequest pageRequest = PageRequest.of(0, limit);

        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByCursor(
                    actorContext.memberId(), actorContext.workspaceKey(), cursorId, pageRequest);
        } else {
            notifications = notificationRepository.findByCursor(
                    actorContext.memberId(), actorContext.workspaceKey(), cursorId, pageRequest);
        }

        List<NotificationResponse> content =
                notifications.stream().map(this::toResponse).toList();

        Long nextCursorId = null;
        if (!content.isEmpty()) {
            nextCursorId = content.getLast().id();
        }

        return CursorPageResponse.of(content, nextCursorId);
    }

    public boolean checkUnreadStatus(WorkspaceMemberContext actorContext) {
        return notificationRepository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                actorContext.memberId(), actorContext.workspaceKey());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .type(notification.getType())
                .data(notification.getMessage().data())
                .entityReference(notification.getEntityReference())
                .actorMemberId(notification.getActorMemberId())
                .actorDisplayName(notification.getActorDisplayName())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
