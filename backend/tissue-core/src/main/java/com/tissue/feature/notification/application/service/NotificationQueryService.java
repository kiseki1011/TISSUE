package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.port.usecase.NotificationQueryUseCase;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.shared.dto.KeysetPageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public KeysetPageResponse<NotificationResponse> getNotifications(
            String workspaceKey, Long actorMemberId, boolean unreadOnly, @Nullable Long keysetId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<Notification> notifications;
        if (unreadOnly) {
            notifications =
                    notificationRepository.findUnreadByKeyset(actorMemberId, workspaceKey, keysetId, pageRequest);
        } else {
            notifications = notificationRepository.findByKeyset(actorMemberId, workspaceKey, keysetId, pageRequest);
        }

        List<NotificationResponse> content =
                notifications.stream().map(this::toResponse).toList();

        Long nextKeysetId = null;
        if (!content.isEmpty()) {
            nextKeysetId = content.getLast().id();
        }

        return KeysetPageResponse.of(content, nextKeysetId);
    }

    @Override
    public boolean checkUnreadStatus(String workspaceKey, Long actorMemberId) {
        return notificationRepository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                actorMemberId, workspaceKey);
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
