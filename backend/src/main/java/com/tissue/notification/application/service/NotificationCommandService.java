package com.tissue.notification.application.service;

import com.tissue.common.vo.EntityReference;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.notification.domain.vo.NotificationMessage;
import com.tissue.notification.infrastructure.repository.NotificationRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageFactory messageFactory;
    private final NotificationProcessor processor;

    @Transactional
    public void createAndSend(
            UUID eventId,
            NotificationType type,
            EntityReference reference,
            Collection<WorkspaceMemberContact> receivers,
            Long actorMemberId,
            @Nullable String actorDisplayName,
            Object... messageArgs) {

        if (receivers.isEmpty()) {
            return;
        }

        NotificationMessage message = messageFactory.createMessage(type, messageArgs);

        List<Notification> notifications = receivers.stream()
                .map(receiver -> Notification.builder()
                        .eventId(eventId)
                        .notificationType(type)
                        .entityReference(reference)
                        .actorMemberId(actorMemberId)
                        .actorDisplayName(actorDisplayName)
                        .receiverMemberId(receiver.memberId())
                        .receiverEmail(receiver.email())
                        .message(message)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);

        // Dispatch to external channels (e.g. Email, Slack) asynchronously
        // In-App is already handled by saving to DB
        processor.process(notifications);
    }
}
