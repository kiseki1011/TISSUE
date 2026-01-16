package com.tissue.notification.application.service;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.infrastructure.repository.NotificationRepository;
import com.tissue.notification.presentation.dto.response.NotificationResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public boolean checkUnreadStatus(String workspaceKey, Long memberId) {
        return notificationRepository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                memberId, workspaceKey);
    }

    private NotificationResponse toResponse(Notification notification) {
        Locale locale = LocaleContextHolder.getLocale();
        NotificationType type = notification.getType();
        Map<String, String> data = notification.getMessage().data();

        String titleKey = "event." + type.name() + ".title";
        String contentKey = "event." + type.name() + ".content";

        String titleTemplate = messageSource.getMessage(titleKey, null, titleKey, locale);
        String contentTemplate = messageSource.getMessage(contentKey, null, contentKey, locale);

        String title = replacePlaceholders(titleTemplate, data);
        String content = replacePlaceholders(contentTemplate, data);

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

    private String replacePlaceholders(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
