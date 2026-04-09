package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.shared.dto.CursorPageResponse;
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
            String workspaceKey, Long memberId, boolean unreadOnly, @Nullable Long cursorId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByCursor(memberId, workspaceKey, cursorId, pageRequest);
        } else {
            notifications = notificationRepository.findByCursor(memberId, workspaceKey, cursorId, pageRequest);
        }

        List<NotificationResponse> content =
                notifications.stream().map(this::toResponse).toList();

        Long nextCursorId = null;
        if (!content.isEmpty()) {
            nextCursorId = content.getLast().id();
        }

        return CursorPageResponse.of(content, nextCursorId);
    }

    public boolean checkUnreadStatus(String workspaceKey, Long memberId) {
        return notificationRepository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                memberId, workspaceKey);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .type(notification.getNotificationType())
                .data(notification.getMessage().data())
                .entityReference(notification.getEntityReference())
                .actorMemberId(notification.getActorMemberId())
                .actorDisplayName(notification.getActorDisplayName())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
