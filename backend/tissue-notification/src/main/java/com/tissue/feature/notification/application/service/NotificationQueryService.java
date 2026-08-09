package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.port.usecase.NotificationQueryUseCase;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.dto.Cursor;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.IdCursor;
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
    public CursorPage<NotificationResponse> getNotifications(
            Long actorMemberId,
            boolean unreadOnly,
            @Nullable List<NotificationType> types,
            @Nullable String cursor,
            int limit) {
        IdCursor decoded = Cursor.decode(cursor, IdCursor.class);
        Long keysetId = (decoded != null) ? decoded.id() : null;

        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Notification> rows = fetchRows(actorMemberId, unreadOnly, types, keysetId, pageRequest);

        boolean hasNext = rows.size() > limit;
        List<Notification> pageRows = hasNext ? rows.subList(0, limit) : rows;

        List<NotificationResponse> content =
                pageRows.stream().map(this::toResponse).toList();
        String nextCursor =
                hasNext ? Cursor.encode(new IdCursor(content.getLast().id())) : null;

        return CursorPage.of(content, nextCursor);
    }

    private List<Notification> fetchRows(
            Long memberId,
            boolean unreadOnly,
            @Nullable List<NotificationType> types,
            @Nullable Long keysetId,
            PageRequest pageRequest) {
        if (types != null && !types.isEmpty()) {
            return notificationRepository.findByKeysetAndTypes(memberId, unreadOnly, types, keysetId, pageRequest);
        }
        return unreadOnly
                ? notificationRepository.findUnreadByKeyset(memberId, keysetId, pageRequest)
                : notificationRepository.findByKeyset(memberId, keysetId, pageRequest);
    }

    @Override
    public boolean checkUnreadStatus(Long actorMemberId) {
        return notificationRepository.existsByReceiverMemberIdAndIsReadFalse(actorMemberId);
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
