package com.tissue.notification.application.service;

import com.tissue.common.dto.CursorPageResponse;
import com.tissue.notification.application.dto.response.NotificationResponse;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    public CursorPageResponse<NotificationResponse> getNotifications(
            String workspaceKey, Long memberId, boolean unreadOnly, @Nullable Long cursorId, int limit) {

        List<Notification> notifications;
        PageRequest pageRequest = PageRequest.of(0, limit);

        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByCursor(memberId, workspaceKey, cursorId, pageRequest);
        } else {
            notifications = notificationRepository.findByCursor(memberId, workspaceKey, cursorId, pageRequest);
        }

        List<NotificationResponse> content =
                notifications.stream().map(this::toResponse).toList();

        Long nextCursorId = null;
        if (!content.isEmpty()) {
            nextCursorId = content.get(content.size() - 1).id();
        }

        return CursorPageResponse.of(content, nextCursorId);
    }

    public boolean checkUnreadStatus(String workspaceKey, Long memberId) {
        return notificationRepository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                memberId, workspaceKey);
    }

    // TODO: Consider abstracting or extracting the render logic
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

    // TODO: Consider using Apache Commons Text - StringSubstitutor
    private String replacePlaceholders(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
