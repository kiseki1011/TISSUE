package com.tissue.feature.notification.application.service;

import static com.tissue.feature.notification.domain.exception.NotificationErrorCode.NOT_YOUR_NOTIFICATION;

import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.port.usecase.NotificationCommandUseCase;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.exception.NotificationNotFoundException;
import com.tissue.feature.notification.domain.service.NotificationMessageFactory;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageFactory messageFactory;
    private final NotificationDispatchService dispatchService;

    @Transactional
    public void createAndSend(
            UUID eventId,
            NotificationType type,
            EntityReference reference,
            Collection<MemberContactInfo> receivers,
            @Nullable Long actorMemberId,
            @Nullable String actorDisplayName,
            Map<String, String> data) {
        if (receivers.isEmpty()) {
            return;
        }

        NotificationMessage message = messageFactory.createMessage(type, data);

        List<Notification> notifications = receivers.stream()
                .map(receiver -> Notification.create(
                        eventId,
                        type,
                        reference,
                        receiver.getMemberId(),
                        receiver.getEmail(),
                        receiver.getLanguage(),
                        message,
                        actorMemberId,
                        actorDisplayName))
                .toList();

        notificationRepository.saveAll(notifications);
        dispatchService.dispatch(notifications);
    }

    @Override
    @Transactional
    public void readNotification(Long notificationId, Long actorMemberId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!Objects.equals(notification.getReceiverMemberId(), actorMemberId)) {
            throw new ForbiddenException(NOT_YOUR_NOTIFICATION);
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void readAllNotifications(Long actorMemberId) {
        notificationRepository.markAllAsRead(actorMemberId);
    }
}
