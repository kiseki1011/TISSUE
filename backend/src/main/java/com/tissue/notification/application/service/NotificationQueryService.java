package com.tissue.notification.application.service;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.infrastructure.repository.NotificationRepository;
import com.tissue.notification.presentation.dto.response.NotificationResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String workspaceKey, Long memberId, boolean unreadOnly) {
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                    memberId, workspaceKey);
        } else {
            notifications =
                    notificationRepository.findByReceiverMemberIdAndEntityReference_WorkspaceKeyOrderByCreatedAtDesc(
                            memberId, workspaceKey);
        }

        return notifications.stream().map(this::toResponse).toList();
    }

    private NotificationResponse toResponse(Notification n) {
        Locale locale = LocaleContextHolder.getLocale();
        NotificationType type = n.getType();
        List<String> args = n.getMessage().args();
        Object[] argArray = args.toArray();

        String titleKey = "event." + type.name() + ".title";
        String contentKey = "event." + type.name() + ".content";

        String title = messageSource.getMessage(titleKey, argArray, titleKey, locale);
        String content = messageSource.getMessage(contentKey, argArray, contentKey, locale);

        return NotificationResponse.builder()
                .id(n.getId())
                .eventId(n.getEventId())
                .type(type)
                .title(title)
                .content(content)
                .entityReference(n.getEntityReference())
                .actorMemberId(n.getActorMemberId())
                .actorDisplayName(n.getActorDisplayName())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
