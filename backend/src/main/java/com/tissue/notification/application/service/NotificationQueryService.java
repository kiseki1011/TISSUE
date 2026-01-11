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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    // TODO: 모든 알림을 가져오는건 성능 문제가 있지 않을까? paging api로 구현하는게 좋을 것 같음
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

    private NotificationResponse toResponse(Notification notification) {
        Locale locale = LocaleContextHolder.getLocale();
        NotificationType type = notification.getType();
        List<String> args = notification.getMessage().args();
        Object[] argArray = args.toArray();

        String titleKey = "event." + type.name() + ".title";
        String contentKey = "event." + type.name() + ".content";

        String title = messageSource.getMessage(titleKey, argArray, titleKey, locale);
        String content = messageSource.getMessage(contentKey, argArray, contentKey, locale);

        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .type(type)
                .title(title)
                .content(content)
                .entityReference(notification.getEntityReference())
                .actorMemberId(notification.getActorMemberId())
                .actorDisplayName(notification.getActorDisplayName())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
